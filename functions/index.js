/**
 * SwiftTrack Backend Cloud Functions
 * Automated Admin Email Notifications for Feedback, Support Tickets, and Problem Reports
 * Admin Target Email: kheroali66@gmail.com
 *
 * NOTE: Credentials (SMTP / SendGrid API Key) are stored exclusively in Firebase Secrets / Environment Variables
 * AND ARE NEVER EXPOSED INSIDE THE ANDROID APPLICATION APK.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const nodemailer = require("nodemailer");

initializeApp();
const db = getFirestore();

const ADMIN_EMAIL = "kheroali66@gmail.com";

// Configure Transport via Environment Secrets (e.g., process.env.SMTP_HOST, process.env.SMTP_USER, process.env.SMTP_PASS)
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || "smtp.gmail.com",
  port: parseInt(process.env.SMTP_PORT || "587"),
  secure: false,
  auth: {
    user: process.env.SMTP_USER || "notifications@swifttrack.app",
    pass: process.env.SMTP_PASS || "SECRET_PASSWORD_IN_FIREBASE_CONSOLE_ONLY",
  },
});

/**
 * 1. App Feedback Trigger
 */
exports.onFeedbackCreated = onDocumentCreated("appFeedback/{feedbackId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const data = snapshot.data();
  const feedbackId = event.params.feedbackId;

  const mailOptions = {
    from: '"SwiftTrack System" <notifications@swifttrack.app>',
    to: ADMIN_EMAIL,
    subject: `[SwiftTrack] New App Feedback - ${data.rating}★ (${data.category})`,
    html: `
      <h2>⭐ New SwiftTrack App Feedback</h2>
      <p><strong>Feedback ID:</strong> ${feedbackId}</p>
      <p><strong>User:</strong> ${data.userName || "N/A"} (${data.userEmail || "Anonymous"})</p>
      <p><strong>User ID:</strong> ${data.userId || "N/A"}</p>
      <hr/>
      <p><strong>Rating:</strong> ${data.rating} / 5 Stars</p>
      <p><strong>Category:</strong> ${data.category}</p>
      <p><strong>Message:</strong></p>
      <blockquote style="background:#f4f4f4; padding:10px; border-left:4px solid #4F46E5;">
        ${data.message || "No comments provided"}
      </blockquote>
      <p><strong>Would Recommend:</strong> ${data.recommend || "N/A"}</p>
      <p><strong>App Version:</strong> ${data.appVersion} (${data.platform})</p>
      <p><strong>Timestamp:</strong> ${new Date(data.createdAtTimestamp || Date.now()).toISOString()}</p>
    `,
  };

  try {
    await transporter.sendMail(mailOptions);
    await snapshot.ref.update({ emailNotificationStatus: "SENT" });
    console.log(`Successfully sent feedback email for ${feedbackId}`);
  } catch (error) {
    console.error(`Failed to send feedback email for ${feedbackId}:`, error);
    await snapshot.ref.update({
      emailNotificationStatus: "FAILED",
      emailError: error.message || "Unknown error",
    });
  }
});

/**
 * 2. Support Ticket Trigger
 */
exports.onSupportTicketCreated = onDocumentCreated("supportTickets/{ticketId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const data = snapshot.data();
  const ticketId = event.params.ticketId;

  const mailOptions = {
    from: '"SwiftTrack Support System" <support@swifttrack.app>',
    to: ADMIN_EMAIL,
    subject: `[SwiftTrack] New Support Request - ${ticketId} (${data.category})`,
    html: `
      <h2>✉️ New Support Request</h2>
      <p><strong>Reference:</strong> ${ticketId}</p>
      <p><strong>User:</strong> ${data.userName || "N/A"} (${data.userEmail || "N/A"})</p>
      <p><strong>User ID:</strong> ${data.userId || "N/A"}</p>
      <hr/>
      <p><strong>Category:</strong> ${data.category}</p>
      <p><strong>Subject:</strong> ${data.subject}</p>
      <p><strong>Description:</strong></p>
      <blockquote style="background:#f4f4f4; padding:10px; border-left:4px solid #EF4444;">
        ${data.description}
      </blockquote>
      <p><strong>Booking / Ticket Reference:</strong> ${data.bookingId || "None"}</p>
      <p><strong>Initial Status:</strong> ${data.status || "OPEN"}</p>
      <p><strong>App Version:</strong> ${data.appVersion} (${data.platform})</p>
      <p><strong>Timestamp:</strong> ${new Date(data.createdAtTimestamp || Date.now()).toISOString()}</p>
    `,
  };

  try {
    await transporter.sendMail(mailOptions);
    await snapshot.ref.update({ emailNotificationStatus: "SENT" });
    console.log(`Successfully sent support email for ticket ${ticketId}`);
  } catch (error) {
    console.error(`Failed to send support email for ticket ${ticketId}:`, error);
    await snapshot.ref.update({
      emailNotificationStatus: "FAILED",
      emailError: error.message || "Unknown error",
    });
  }
});

/**
 * 3. Problem Report Trigger
 */
exports.onProblemReportCreated = onDocumentCreated("problemReports/{reportId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const data = snapshot.data();
  const reportId = event.params.reportId;

  const mailOptions = {
    from: '"SwiftTrack Bug Reporting" <bugs@swifttrack.app>',
    to: ADMIN_EMAIL,
    subject: `[SwiftTrack] New Problem Report - ${reportId} (${data.category})`,
    html: `
      <h2>⚠️ New Problem Report</h2>
      <p><strong>Report ID:</strong> ${reportId}</p>
      <p><strong>User:</strong> ${data.userName || "N/A"} (${data.userEmail || "N/A"})</p>
      <hr/>
      <p><strong>Category:</strong> ${data.category}</p>
      <p><strong>Subject:</strong> ${data.subject}</p>
      <p><strong>Description:</strong></p>
      <blockquote style="background:#fff3cd; padding:10px; border-left:4px solid #F59E0B;">
        ${data.description}
      </blockquote>
      <p><strong>Screen:</strong> ${data.screenName || "N/A"}</p>
      <p><strong>Booking Ref:</strong> ${data.bookingId || "None"}</p>
      <p><strong>Device Model:</strong> ${data.deviceModel || "Unknown"}</p>
      <p><strong>Android Version:</strong> ${data.androidVersion || "Unknown"}</p>
      <p><strong>App Version:</strong> ${data.appVersion}</p>
      <p><strong>Timestamp:</strong> ${new Date(data.createdAtTimestamp || Date.now()).toISOString()}</p>
    `,
  };

  try {
    await transporter.sendMail(mailOptions);
    await snapshot.ref.update({ emailNotificationStatus: "SENT" });
    console.log(`Successfully sent problem report email for ${reportId}`);
  } catch (error) {
    console.error(`Failed to send problem report email for ${reportId}:`, error);
    await snapshot.ref.update({
      emailNotificationStatus: "FAILED",
      emailError: error.message || "Unknown error",
    });
  }
});
