package com.swifttrack.app.presentation.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.swifttrack.app.R;

public class LoadingOverlayDialog extends Dialog {

    private String message;
    private TextView tvMessage;

    public LoadingOverlayDialog(@NonNull Context context) {
        super(context);
        this.message = "Please Wait...";
    }

    public LoadingOverlayDialog(@NonNull Context context, String message) {
        super(context);
        this.message = message != null ? message : "Please Wait...";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_loading_overlay);
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        tvMessage = findViewById(R.id.tv_loading_message);
        if (tvMessage != null) {
            tvMessage.setText(message);
        }
    }

    public void setMessage(String newMessage) {
        this.message = newMessage;
        if (tvMessage != null) {
            tvMessage.setText(newMessage);
        }
    }
}
