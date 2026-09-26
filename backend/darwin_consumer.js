/**
 * SwiftTrack Backend Service - National Rail RDM Darwin Push Consumer & Firestore Live State Engine
 *
 * Flow:
 * RDM Darwin Push / Kafka Stream -> KafkaJS Consumer -> Darwin Message Parser
 * -> Normalized Live State Processor -> Firestore liveServices/{serviceId}
 *
 * Credentials are read from backend/.env only. Do not commit real credentials,
 * service account JSON files, or certificates.
 */

require('dotenv').config();

const { Kafka, logLevel } = require('kafkajs');
const { XMLParser } = require('fast-xml-parser');
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const dns = require('dns').promises;

const PLACEHOLDER_VALUES = new Set([
  'your_rdm_consumer_username',
  'your_rotated_rdm_consumer_password',
  'your_rdm_consumer_password'
]);

function envFlag(name) {
  return ['1', 'true', 'yes', 'on'].includes(String(process.env[name] || '').toLowerCase());
}

function isBlankOrPlaceholder(value) {
  return !value || PLACEHOLDER_VALUES.has(String(value).trim());
}

function resolveBackendPath(relativeOrAbsolutePath) {
  return path.isAbsolute(relativeOrAbsolutePath)
    ? relativeOrAbsolutePath
    : path.resolve(__dirname, relativeOrAbsolutePath);
}

function validateExistingFile(envName, description, errors) {
  const configuredPath = process.env[envName];
  if (!configuredPath) {
    errors.push(`${envName} is required for ${description}.`);
    return null;
  }

  const resolvedPath = resolveBackendPath(configuredPath);
  if (!fs.existsSync(resolvedPath)) {
    errors.push(`${description} was not found at ${configuredPath}.`);
    return null;
  }

  return resolvedPath;
}

function failStartup(err) {
  console.error('Startup configuration error:');
  console.error(err.message);
  console.error('Fix backend/.env and the credential files, then run npm start again.');
  process.exitCode = 1;
}

console.log('====================================================');
console.log('SwiftTrack RDM Darwin Push Live Engine Starting...');
console.log('====================================================');

const bootstrapServers = (process.env.DARWIN_BOOTSTRAP_SERVERS || 'kafka.rail-data-marketplace.co.uk:9092')
  .split(',')
  .map((server) => server.trim())
  .filter(Boolean);
const topicName = process.env.DARWIN_TOPIC || 'darwin.push.trains';
const username = process.env.DARWIN_USERNAME;
const password = process.env.DARWIN_PASSWORD;
const rawGroup = process.env.DARWIN_CONSUMER_GROUP || `${username || 'MXSYDRIEY7OW7ZHY'}-swifttrack`;
const groupId = rawGroup.includes('-') && !rawGroup.endsWith('-group') ? rawGroup : `${rawGroup}-${Date.now()}`;
const securityProtocol = process.env.DARWIN_SECURITY_PROTOCOL || 'SASL_SSL';
const saslMechanism = (process.env.DARWIN_SASL_MECHANISM || 'plain').toLowerCase();
const simulatorEnabled = envFlag('ENABLE_DARWIN_SIMULATOR');
const checkMode = process.argv.includes('--check');

const CRS_NAMES = {
  PAD: 'London Paddington',
  HWV: 'Heathrow Terminal 5',
  HXX: 'Heathrow Terminals 2 & 3',
  HAF: 'Heathrow Airport Central',
  HHY: 'Heathrow Terminal 4',
  HT5: 'Heathrow Terminal 5'
};
const SUPPORTED_CRS = new Set(Object.keys(CRS_NAMES));

let firestore = null;
let caCertificatePath = null;

try {
  const startupErrors = [];

  if (bootstrapServers.length === 0) {
    startupErrors.push('DARWIN_BOOTSTRAP_SERVERS must contain at least one Kafka broker.');
  }

  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH || './config/serviceAccountKey.json';
  const resolvedServiceAccountPath = validateExistingFile(
    'FIREBASE_SERVICE_ACCOUNT_PATH',
    'Firebase service account key',
    startupErrors
  );
  caCertificatePath = validateExistingFile('DARWIN_CA_CERTIFICATE_PATH', 'Darwin RDM CA certificate', startupErrors);

  if (!simulatorEnabled && (isBlankOrPlaceholder(username) || isBlankOrPlaceholder(password))) {
    startupErrors.push('DARWIN_USERNAME and DARWIN_PASSWORD must contain real RDM credentials.');
  }

  if (startupErrors.length > 0) {
    throw new Error(startupErrors.map((message) => `- ${message}`).join('\n'));
  }

  const serviceAccount = require(resolvedServiceAccountPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    projectId: process.env.FIREBASE_PROJECT_ID || serviceAccount.project_id || 'swifttrack-6bc00'
  });
  firestore = admin.firestore();

  console.log(`Firebase Admin SDK initialized with ${serviceAccountPath}`);
  console.log(`Target Kafka Server: ${bootstrapServers.join(', ')}`);
  console.log(`Target Topic: ${topicName}`);
  console.log(`Consumer Group: ${groupId}`);
  console.log(`Security Protocol: ${securityProtocol} (${saslMechanism.toUpperCase()})`);
} catch (err) {
  failStartup(err);
  return;
}

if (simulatorEnabled) {
  console.log('====================================================');
  console.log('ENABLE_DARWIN_SIMULATOR=true, starting simulator instead of real Kafka.');
  console.log('====================================================');
  startDarwinLiveSimulatorEngine();
} else if (checkMode) {
  checkRealKafkaDarwinConnection();
} else {
  startRealKafkaDarwinConsumer();
}

function createKafkaClient() {
  return new Kafka({
    clientId: 'swifttrack-backend-consumer',
    brokers: bootstrapServers,
    ssl: {
      rejectUnauthorized: true,
      ca: [fs.readFileSync(caCertificatePath, 'utf-8')]
    },
    sasl: {
      mechanism: saslMechanism,
      username,
      password
    },
    connectionTimeout: Number(process.env.DARWIN_CONNECTION_TIMEOUT_MS || 15000),
    authenticationTimeout: Number(process.env.DARWIN_AUTH_TIMEOUT_MS || 15000),
    requestTimeout: Number(process.env.DARWIN_REQUEST_TIMEOUT_MS || 30000),
    logLevel: logLevel.INFO
  });
}

async function checkRealKafkaDarwinConnection() {
  const kafka = createKafkaClient();
  const consumer = kafka.consumer({ groupId: `${groupId}-check-${Date.now()}` });
  try {
    console.log(`Loaded RDM CA Certificate from ${process.env.DARWIN_CA_CERTIFICATE_PATH}`);
    await checkBrokerDns();
    console.log('Checking Darwin Kafka connection and topic subscription...');
    await consumer.connect();
    await consumer.subscribe({ topic: topicName, fromBeginning: false });
    console.log('Darwin Kafka check passed: connected and subscribed.');
    await consumer.disconnect();
    process.exitCode = 0;
  } catch (err) {
    console.error('Darwin Kafka check failed:', err.message);
    process.exitCode = 1;
    try {
      await consumer.disconnect();
    } catch (_) {
      // ignore disconnect errors during failed startup checks
    }
  }
}

async function checkBrokerDns() {
  for (const broker of bootstrapServers) {
    const host = broker.split(':')[0];
    try {
      const result = await dns.lookup(host);
      console.log(`DNS OK: ${host} -> ${result.address}`);
    } catch (err) {
      throw new Error(
        `Cannot resolve Darwin Kafka broker host "${host}". ` +
        'Update DARWIN_BOOTSTRAP_SERVERS to the exact bootstrap broker host provided by your RDM Darwin product subscription, ' +
        'or connect to the required network/VPN if RDM provided a private hostname.'
      );
    }
  }
}

async function startRealKafkaDarwinConsumer() {
  try {
    const kafka = createKafkaClient();
    const consumer = kafka.consumer({ groupId: process.env.DARWIN_CONSUMER_GROUP || 'SC-3c060984-ea61-4ca6-bdae-0a5db2975994' });
    const xmlParser = new XMLParser({ ignoreAttributes: false, attributeNamePrefix: '@_' });

    console.log(`Loaded RDM CA Certificate from ${process.env.DARWIN_CA_CERTIFICATE_PATH}`);
    console.log('Connecting to Darwin Kafka Broker...');
    await consumer.connect();
    console.log('CONNECTED SUCCESSFULLY to National Rail RDM Darwin Stream.');

    await consumer.subscribe({ topic: topicName, fromBeginning: false });
    console.log('Subscribed to topic:', topicName);

    consumer.on(consumer.events.GROUP_JOIN, async () => {
      try {
        const admin = kafka.admin();
        await admin.connect();
        const offsets = await admin.fetchTopicOffsets(topicName);
        if (offsets && offsets.length > 0) {
          const high = BigInt(offsets[0].high || '0');
          const seekTarget = String(high > 3000n ? high - 3000n : 0n);
          await admin.disconnect();
          consumer.seek({ topic: topicName, partition: 0, offset: seekTarget });
          console.log(`✓ Seeked consumer to recent offset ${seekTarget} (High: ${high})`);
        }
      } catch (e) {
        console.warn('⚠️ Group join seek warning:', e.message);
      }
    });

    console.log('🟢 Waiting for live Darwin messages...');

    await consumer.run({
      eachMessage: async ({ message }) => {
        try {
          const rawMessageStr = message.value.toString();
          let parsedData = rawMessageStr.trim().startsWith('<')
            ? xmlParser.parse(rawMessageStr)
            : JSON.parse(rawMessageStr);

          // Unwrap Confluent Cloud JSON wrapper if present
          if (parsedData && parsedData.bytes && typeof parsedData.bytes === 'string') {
            try {
              parsedData = JSON.parse(parsedData.bytes);
            } catch (_) {}
          }

          const normalizedServices = normalizeDarwinMessage(parsedData);
          for (const normalizedService of normalizedServices) {
            if (normalizedService && firestore) {
              await syncServiceToFirestore(normalizedService);
            }
          }
        } catch (err) {
          console.error('Error processing message:', err.message);
        }
      }
    });
  } catch (err) {
    console.error('Failed to connect to Darwin Kafka:', err.message);
    process.exitCode = 1;
  }
}

function asArray(value) {
  if (value == null) return [];
  return Array.isArray(value) ? value : [value];
}

function attr(obj, name) {
  if (!obj || typeof obj !== 'object') return undefined;
  return obj[`@_${name}`] ?? obj[name];
}

function parseDarwinTime(value, fallbackDate) {
  if (!value) return null;
  const raw = String(value).trim();
  const direct = Date.parse(raw);
  if (!Number.isNaN(direct)) return direct;

  const hhmm = raw.match(/^(\d{1,2}):(\d{2})(?::\d{2})?$/);
  if (!hhmm) return null;

  const base = fallbackDate ? new Date(fallbackDate) : new Date();
  base.setHours(Number(hhmm[1]), Number(hhmm[2]), 0, 0);
  return base.getTime();
}

function getServiceUpdates(rawData) {
  const pport = rawData?.Pport || rawData?.pport || rawData;
  const updates = [
    ...asArray(pport?.uR),
    ...asArray(pport?.UR),
    ...asArray(pport?.trainUpdate),
    ...asArray(rawData?.uR),
    ...asArray(rawData?.UR)
  ];
  return updates.length > 0 ? updates : [rawData];
}

function getTrainService(update) {
  return update?.TS || update?.ts || update?.TrainStatus || update;
}

function getLocations(trainService) {
  return [
    ...asArray(trainService?.Location),
    ...asArray(trainService?.location),
    ...asArray(trainService?.locations?.Location),
    ...asArray(trainService?.locations?.location)
  ];
}

function locationCrs(location) {
  return String(attr(location, 'tpl') || attr(location, 'crs') || attr(location, 'location') || '').toUpperCase();
}

function normalizeStatus(cancelled, scheduledMs, estimatedMs) {
  if (cancelled) return 'CANCELLED';
  if (scheduledMs && estimatedMs && estimatedMs > scheduledMs) return 'DELAYED';
  return 'ON_TIME';
}

function normalizeDarwinMessage(rawData) {
  if (!rawData) return [];

  try {
    const now = Date.now();
    const services = [];

    for (const update of getServiceUpdates(rawData)) {
      const trainService = getTrainService(update);
      const locations = getLocations(trainService);

      if (locations.length === 0) {
        const flatService = normalizeFlatMessage(update, now);
        if (flatService) services.push(flatService);
        continue;
      }

      const supportedLocations = locations.filter((location) => SUPPORTED_CRS.has(locationCrs(location)));
      if (supportedLocations.length === 0) continue;

      const rid = attr(trainService, 'rid') || attr(update, 'rid') || attr(rawData?.Pport, 'rid');
      const uid = attr(trainService, 'uid') || attr(update, 'uid');
      const serviceId = String(rid || uid || `darwin-${now}-${services.length}`);
      const cancelled = String(attr(trainService, 'isCancelled') || attr(update, 'isCancelled') || '').toLowerCase() === 'true';
      const toc = attr(trainService, 'toc') || attr(trainService, 'tocName') || 'Heathrow Express';
      const serviceDate = attr(trainService, 'ssd') || attr(update, 'ssd') || attr(rawData?.Pport, 'ts') || now;

      for (const location of supportedLocations) {
        const originCode = locationCrs(location);
        const scheduledDeparture =
          parseDarwinTime(attr(location, 'wtd') || attr(location, 'ptd') || attr(location, 'std'), serviceDate) ||
          parseDarwinTime(attr(location, 'wta') || attr(location, 'pta') || attr(location, 'sta'), serviceDate);
        if (!scheduledDeparture) continue;

        const estimatedDeparture =
          parseDarwinTime(attr(location, 'etd') || attr(location, 'dep') || attr(location, 'atd'), serviceDate) ||
          scheduledDeparture;

        const destinationCode = inferDestinationCode(originCode, locations);
        if (!destinationCode) continue;

        const delayMinutes = Math.max(0, Math.round((estimatedDeparture - scheduledDeparture) / 60000));
        services.push({
          serviceId: `${serviceId}-${originCode}`,
          rid: rid ? String(rid) : null,
          uid: uid ? String(uid) : null,
          originCode,
          originName: CRS_NAMES[originCode],
          destinationCode,
          destinationName: CRS_NAMES[destinationCode],
          scheduledDeparture,
          estimatedDeparture,
          platform: String(attr(location, 'plat') || attr(location, 'platform') || '').trim() || null,
          status: normalizeStatus(cancelled, scheduledDeparture, estimatedDeparture),
          delayMinutes,
          cancelled,
          operator: String(toc),
          lastUpdatedMs: now
        });
      }
    }

    return services;
  } catch (err) {
    console.error('Failed to normalize Darwin message:', err.message);
    return [];
  }
}

function inferDestinationCode(originCode, allLocations) {
  const routeCodes = allLocations.map(locationCrs).filter(Boolean);
  if (originCode === 'PAD') {
    if (routeCodes.includes('HWV')) return 'HWV';
    if (routeCodes.includes('HXX')) return 'HXX';
    if (routeCodes.includes('HAF')) return 'HAF';
    return 'HWV';
  }
  if (['HWV', 'HXX', 'HAF', 'HHY', 'HT5'].includes(originCode)) {
    return 'PAD';
  }
  return null;
}

function normalizeFlatMessage(rawData, now) {
  const originCode = String(rawData.originCode || rawData.origin || '').toUpperCase();
  const destinationCode = String(rawData.destinationCode || rawData.destination || rawData.dest || '').toUpperCase();
  if (!SUPPORTED_CRS.has(originCode) || !SUPPORTED_CRS.has(destinationCode)) return null;

  const scheduledDeparture = parseDarwinTime(rawData.scheduledDeparture || rawData.std, now);
  const estimatedDeparture = parseDarwinTime(rawData.estimatedDeparture || rawData.etd, now) || scheduledDeparture;
  if (!scheduledDeparture) return null;

  const cancelled = Boolean(rawData.cancelled || rawData.isCancelled);
  const delayMinutes = Math.max(0, Math.round((estimatedDeparture - scheduledDeparture) / 60000));

  return {
    serviceId: String(rawData.serviceId || rawData.rid || `flat-${originCode}-${scheduledDeparture}`),
    rid: rawData.rid ? String(rawData.rid) : null,
    uid: rawData.uid ? String(rawData.uid) : null,
    originCode,
    originName: CRS_NAMES[originCode],
    destinationCode,
    destinationName: CRS_NAMES[destinationCode],
    scheduledDeparture,
    estimatedDeparture,
    platform: rawData.platform ? String(rawData.platform) : null,
    status: normalizeStatus(cancelled, scheduledDeparture, estimatedDeparture),
    delayMinutes,
    cancelled,
    operator: rawData.operator || 'Heathrow Express',
    lastUpdatedMs: now
  };
}

async function syncServiceToFirestore(service) {
  if (!firestore || !service || !service.serviceId) return;

  try {
    const docRef = firestore.collection('liveServices').doc(service.serviceId);
    await docRef.set({
      serviceId: service.serviceId,
      originCode: service.originCode,
      originName: service.originName,
      destinationCode: service.destinationCode,
      destinationName: service.destinationName,
      scheduledDeparture: service.scheduledDeparture,
      estimatedDeparture: service.estimatedDeparture,
      platform: service.platform,
      status: service.status,
      delayMinutes: service.delayMinutes,
      cancelled: service.cancelled,
      operator: service.operator,
      rid: service.rid || null,
      uid: service.uid || null,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      lastUpdatedMs: service.lastUpdatedMs
    }, { merge: true });

    console.log(
      `Firestore updated liveServices/${service.serviceId} ` +
      `[${service.originCode} -> ${service.destinationCode}] ` +
      `Platform ${service.platform} | Status: ${service.status}`
    );
  } catch (err) {
    console.error(`Firestore sync error for ${service.serviceId}:`, err.message);
  }
}

function startDarwinLiveSimulatorEngine() {
  console.log('Darwin Live State Simulator active. Syncing every 15 seconds to Firestore liveServices.');

  const updateSimulatedServices = async () => {
    if (!firestore) return;
    const now = Date.now();

    const services = [
      {
        serviceId: 'HEX_PAD_LHR_01',
        originCode: 'PAD',
        originName: 'London Paddington',
        destinationCode: 'HWV',
        destinationName: 'Heathrow Airport',
        scheduledDeparture: now + 4 * 60 * 1000,
        estimatedDeparture: now + 4 * 60 * 1000,
        platform: '8',
        status: 'ON_TIME',
        delayMinutes: 0,
        cancelled: false,
        operator: 'Heathrow Express',
        lastUpdatedMs: now
      },
      {
        serviceId: 'HEX_LHR_PAD_01',
        originCode: 'HWV',
        originName: 'Heathrow Airport',
        destinationCode: 'PAD',
        destinationName: 'London Paddington',
        scheduledDeparture: now + 12 * 60 * 1000,
        estimatedDeparture: now + 12 * 60 * 1000,
        platform: '2',
        status: 'ON_TIME',
        delayMinutes: 0,
        cancelled: false,
        operator: 'Heathrow Express',
        lastUpdatedMs: now
      },
      {
        serviceId: 'HEX_PAD_LHR_02',
        originCode: 'PAD',
        originName: 'London Paddington',
        destinationCode: 'HWV',
        destinationName: 'Heathrow Airport',
        scheduledDeparture: now + 19 * 60 * 1000,
        estimatedDeparture: now + 21 * 60 * 1000,
        platform: '7',
        status: 'DELAYED',
        delayMinutes: 2,
        cancelled: false,
        operator: 'Heathrow Express',
        lastUpdatedMs: now
      }
    ];

    for (const service of services) {
      await syncServiceToFirestore(service);
    }
  };

  updateSimulatedServices();
  setInterval(updateSimulatedServices, 15000);
}
