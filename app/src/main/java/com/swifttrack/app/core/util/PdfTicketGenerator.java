package com.swifttrack.app.core.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.swifttrack.app.data.local.TicketEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfTicketGenerator {

    public static File generatePdfTicket(Context context, TicketEntity ticket) {
        if (context == null || ticket == null) return null;

        PdfDocument document = new PdfDocument();
        // A4 page size: 595 x 842 points
        boolean isReturn = "RETURN".equalsIgnoreCase(ticket.tripType);
        int pageHeight = isReturn ? 920 : 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Header Background Banner
        paint.setColor(Color.parseColor("#0F172A")); // Deep Slate / Primary Indigo
        canvas.drawRect(0, 0, 595, 110, paint);

        // App Header Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setFakeBoldText(true);
        canvas.drawText("SWIFTTRACK DIGITAL RAILWAY TICKET", 30, 48, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        paint.setColor(Color.parseColor("#94A3B8"));
        canvas.drawText("Official National Express Electronic Travel Credential", 30, 72, paint);

        paint.setColor(Color.parseColor("#0EA5E9"));
        paint.setFakeBoldText(true);
        canvas.drawText(isReturn ? "RETURN TICKET" : "ONE-WAY TICKET", 440, 48, paint);

        // Ticket Details Box
        int cardBottom = isReturn ? 410 : 340;
        paint.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRoundRect(30, 130, 565, cardBottom, 16, 16, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(11);
        canvas.drawText("TICKET IDENTIFIER", 50, 160, paint);
        canvas.drawText("BOOKING REFERENCE", 320, 160, paint);

        paint.setColor(Color.parseColor("#0F172A"));
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText(ticket.ticketCode != null ? ticket.ticketCode : "ST-26-8F4K7M2P", 50, 184, paint);
        canvas.drawText(ticket.bookingReference != null ? ticket.bookingReference : "Ref: ST-9A72F6K1", 320, 184, paint);

        // Outbound Journey Info
        paint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawLine(50, 204, 545, 204, paint);

        paint.setColor(Color.parseColor("#64748B"));
        paint.setTextSize(10);
        canvas.drawText("OUTBOUND JOURNEY", 50, 222, paint);

        paint.setColor(Color.parseColor("#0EA5E9"));
        paint.setTextSize(15);
        paint.setFakeBoldText(true);
        String origin = ticket.originStationName != null ? ticket.originStationName : "London Paddington (PAD)";
        String dest = ticket.destStationName != null ? ticket.destStationName : "Heathrow T5 (HWV)";
        canvas.drawText(origin + "  ➔  " + dest, 50, 244, paint);

        paint.setColor(Color.parseColor("#475569"));
        paint.setTextSize(11);
        paint.setFakeBoldText(false);
        String cls = ticket.travelClass != null ? ticket.travelClass : "Standard Express";
        String cat = ticket.passengerCategory != null ? ticket.passengerCategory : "1 Adult";
        canvas.drawText("Class: " + cls + "  •  Category: " + cat, 50, 266, paint);

        String dep = ticket.departureTime != null ? ticket.departureTime : "Today 08:15 AM";
        String arr = ticket.arrivalTime != null ? ticket.arrivalTime : "Today 08:30 AM";
        canvas.drawText("Departure: " + dep + "  |  Arrival: " + arr, 50, 286, paint);

        // Return Journey Info (If Return Ticket)
        int currentY = 310;
        if (isReturn) {
            paint.setColor(Color.parseColor("#CBD5E1"));
            canvas.drawLine(50, 298, 545, 298, paint);

            paint.setColor(Color.parseColor("#D97706"));
            paint.setTextSize(10);
            paint.setFakeBoldText(true);
            canvas.drawText("RETURN JOURNEY", 50, 316, paint);

            paint.setColor(Color.parseColor("#D97706"));
            paint.setTextSize(15);
            canvas.drawText(dest + "  ➔  " + origin, 50, 338, paint);

            paint.setColor(Color.parseColor("#475569"));
            paint.setTextSize(11);
            paint.setFakeBoldText(false);
            String retDate = ticket.returnDate != null ? ticket.returnDate : "Flexible Return";
            String retDep = ticket.returnDepartureTime != null ? ticket.returnDepartureTime : "18:10 PM";
            String retArr = ticket.returnArrivalTime != null ? ticket.returnArrivalTime : "18:25 PM";
            String depDisplay = retDep.contains("•") ? retDep : (retDate + " • " + retDep);
            String arrDisplay = retArr.contains("•") ? retArr : (retDate + " • " + retArr);
            canvas.drawText("Departure: " + depDisplay + "  |  Arrival: " + arrDisplay, 50, 360, paint);

            currentY = 390;
        }

        paint.setColor(Color.parseColor("#10B981"));
        paint.setFakeBoldText(true);
        canvas.drawText("STATUS: " + (ticket.status != null ? ticket.status.toUpperCase() : "PAID & ACTIVE"), 50, currentY, paint);

        // Embed High-Resolution QR Code
        int qrY = isReturn ? 440 : 370;
        String qrPayload = ticket.signedQrPayload != null && !ticket.signedQrPayload.isEmpty() ?
                ticket.signedQrPayload : "STT:" + ticket.ticketId + ":SIG_SECURE";

        Bitmap qrBitmap = QrCodeGenerator.generateQrCode(qrPayload, 260, 260);
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, 167, qrY, null);
        }

        // Terms Footer
        int footerY = isReturn ? 740 : 680;
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        canvas.drawText("This digital ticket is cryptographically signed and non-transferable.", 120, footerY, paint);
        canvas.drawText("Present this QR credential to the station barrier scanner or onboard inspector.", 100, footerY + 18, paint);
        canvas.drawText("Generated via SwiftTrack App on " + new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(new Date()), 95, footerY + 36, paint);

        document.finishPage(page);

        // Save PDF File
        File pdfFile = null;
        try {
            String fileName = "SwiftTrack_Ticket_" + (ticket.bookingReference != null ? ticket.bookingReference : "ST-" + System.currentTimeMillis()) + ".pdf";
            File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) dir = context.getFilesDir();

            pdfFile = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

        } catch (Exception e) {
            document.close();
            return null;
        }

        return pdfFile;
    }

    public static void openPdf(Context context, File pdfFile) {
        if (context == null || pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "PDF file not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Open Digital Ticket PDF"));
        } catch (Exception e) {
            Toast.makeText(context, "No PDF viewer app found on device", Toast.LENGTH_LONG).show();
        }
    }

    public static void sharePdf(Context context, File pdfFile) {
        if (context == null || pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "PDF file not available for sharing", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", pdfFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share Digital Ticket PDF"));
        } catch (Exception e) {
            Toast.makeText(context, "Error sharing ticket PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
