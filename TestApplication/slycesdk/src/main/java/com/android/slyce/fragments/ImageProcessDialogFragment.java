package com.android.slyce.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.slyce.Slyce;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import com.android.slyce.requests.SlyceProductsRequest;
import com.android.slyce.roundedimage.RoundedImageView;
import com.android.slyce.utils.BitmapLoader;
import com.android.slycesdk.R;

import org.json.JSONArray;

import java.lang.ref.WeakReference;

/**
 * Use the {@link ImageProcessDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageProcessDialogFragment extends DialogFragment implements View.OnClickListener {

    private static final String ARG_PROCESS_TYPE = "arg_process_type";
    private static final String ARG_IMAGE_DECODABLE_STRING = "arg_image_decodable_string";

    public static final String GALLERY_BITMAP = "gallery_bitmap";
    public static final String CAMERA_BITMAP = "camera_bitmap";

    private static final String BEGIN_SENDING_IMAGE = "begin_sending_image";
    private static final String BEGIN_ANALYZE_IMAGE = "begin_analyze_image";
    private static final String FINISH_ANALYZE_IMAGE = "finish_analyze_image";

    private static final int UPLOAD_IMAGE_TOTAL_PROGRESS_TIME = 3000;

    private String mProcessType;
    private String mImageDecodableString;

    /* views */
    private Button cancelButton;

    private RoundedImageView mImage;
    private ImageView sendDoneImage;
    private ImageView analyzeDoneImage;

    private ProgressBar horizontalProgressBar;
    private ProgressBar progressSendingImage;
    private ProgressBar progressAnalyzeImage;
    private ProgressBar imageProgressBar;

    private TextView progressMsg;
    private TextView sendImageText;
    private TextView analyzeImageText;

    /* for searching products from gallery image */
    private SlyceProductsRequest mSlyceRequest;

    private UpdateProgressBarAsyncTask task;

    private Uri selecedGalleryImageUri;

    private OnImageProcessDialogFragmentListener mOnImageProcessDialogFragmentListener;

    /** Handles {@link OnSlyceRequestListener} callbacks and sends them to {@link SlyceCameraFragment} */
    public interface OnImageProcessDialogFragmentListener {

        void onImageProcessBarcodeRecognition(SlyceBarcode barcode);

        void onImageProcess2DRecognition(String irid, String productInfo);

        void onImageProcess2DExtendedRecognition(JSONArray products);

        void onImageProcess3DRecognition(JSONArray products);

        void onImageProcessDialogFragmentDismiss();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param processType Parameter 1.
     * @param imageDecodableString Parameter 2.
     * @return A new instance of fragment ImageProcessDialogFragment.
     */
    public static ImageProcessDialogFragment newInstance(String processType, String imageDecodableString) {
        ImageProcessDialogFragment fragment = new ImageProcessDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROCESS_TYPE, processType);
        args.putString(ARG_IMAGE_DECODABLE_STRING, imageDecodableString);
        fragment.setArguments(args);
        return fragment;
    }

    public ImageProcessDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mProcessType = getArguments().getString(ARG_PROCESS_TYPE);
            mImageDecodableString = getArguments().getString(ARG_IMAGE_DECODABLE_STRING);
        }

        setStyle(2, 0);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_image_process_dialog, container, false);

        initRoundedImage(root);

        horizontalProgressBar = (ProgressBar) root.findViewById(R.id.horizontal_progress_bar);
        horizontalProgressBar.setIndeterminate(false);

        progressMsg = (TextView) root.findViewById(R.id.scan_status_main);

        progressSendingImage = (ProgressBar) root.findViewById(R.id.progress_sending_image);
        progressAnalyzeImage = (ProgressBar) root.findViewById(R.id.progress_analyzing_image);

        sendDoneImage = (ImageView) root.findViewById(R.id.done_sending_image);
        analyzeDoneImage = (ImageView) root.findViewById(R.id.done_analyzing_image);

        sendImageText = (TextView) root.findViewById(R.id.text_sending_image);
        analyzeImageText = (TextView) root.findViewById(R.id.text_analyzing_image);

        imageProgressBar = (ProgressBar) root.findViewById(R.id.image_progress_bar);

        cancelButton = (Button) root.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(this);

        updateProgressInfo("");

        if(mProcessType == GALLERY_BITMAP){

            BitmapWorkerTask loader = new BitmapWorkerTask(mImage, imageProgressBar);
            loader.execute(mImageDecodableString);

        }else{
            // mProcessType = CAMERA_BITMAP
            // Set the snapped bitmap to ImageView when its ready
        }

        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.cancel_button){
            dismiss();
        }else if(v.getId() == R.id.scan_not_found_button_done){
            dismiss();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // Cancel SlyceProductsRequest
        if(mSlyceRequest != null){
            mSlyceRequest.cancel();
        }

        // Notify SlyceCameraFragment to cancel SlyceCamera cause ImageProcessDialogFragment dismissed
        mOnImageProcessDialogFragmentListener.onImageProcessDialogFragmentDismiss();

        super.onDismiss(dialog);
    }

    /* PUBLIC METHODS */
    public void setmOnImageProcessDialogFragmentListener(OnImageProcessDialogFragmentListener listener){
        mOnImageProcessDialogFragmentListener = listener;
    }

    public void showDialogFragment(){
        // Create and show the dialog.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        NotFoundDialogFragment newFragment = NotFoundDialogFragment.newInstance();
        newFragment.show(ft, "NotFoundDialogFragment");

        dismiss();
    }

    public void onSnap(Bitmap bitmap) {

        mImage.setImageBitmap(bitmap);
        updateProgressInfo(BEGIN_SENDING_IMAGE);
    }

    public void onImageStartRequest() {
        updateProgressInfo(BEGIN_ANALYZE_IMAGE);
    }

    public void onProgress(long progress, String message) {
        horizontalProgressBar.setProgress(50 + (int) progress / 2);
        progressMsg.setText(message);
    }

    public void onCamera3DRecognition(JSONArray products) {
        updateProgressInfo(FINISH_ANALYZE_IMAGE);

        if(products.length() > 0){
            dismiss();
        }else{
            showDialogFragment();
        }
    }

    public void onError(String message) {
        updateProgressInfo("");

        showDialogFragment();
    }
    /* End */

    /* PRIVATE METHODS */
    private void initRoundedImage(View root){
        mImage = (RoundedImageView) root.findViewById(R.id.image);
        mImage.setCornerRadius(getResources().getDimension(R.dimen.fragment_image_process_image_corners_radius));
        mImage.setBorderColor(getResources().getColor(R.color.image_border_color));
        mImage.setBorderWidth(getResources().getDimension(R.dimen.fragment_image_process_image_border_width));
        mImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mImage.setOval(false);
    }

    /* Invoke this method after an Image was picked from Gallery */
    private void performSlyceProductsRequest(Bitmap bitmap){

        mSlyceRequest = new SlyceProductsRequest(Slyce.get(), new OnSlyceRequestListener() {

            @Override
            public void onBarcodeRecognition(SlyceBarcode barcode) {

                // Update progress bar
                updateProgressInfo(FINISH_ANALYZE_IMAGE);

                // Notify SlyceCameraFragment
                mOnImageProcessDialogFragmentListener.onImageProcessBarcodeRecognition(barcode);

                dismiss();
            }

            @Override
            public void onSlyceProgress(long progress, String message, String id) {
                horizontalProgressBar.setProgress(50 + (int) progress / 2);
                progressMsg.setText(message);
            }

            @Override
            public void on2DRecognition(String irid, String productInfo) {

                // Notify SlyceCameraFragment
                mOnImageProcessDialogFragmentListener.onImageProcess2DRecognition(irid, productInfo);
            }

            @Override
            public void on2DExtendedRecognition(JSONArray products) {

                // Notify SlyceCameraFragment
                mOnImageProcessDialogFragmentListener.onImageProcess2DExtendedRecognition(products);
            }

            @Override
            public void on3DRecognition(JSONArray products) {

                // Update progress bar
                updateProgressInfo(FINISH_ANALYZE_IMAGE);

                // Notify SlyceCameraFragment
                mOnImageProcessDialogFragmentListener.onImageProcess3DRecognition(products);

                if(products.length() > 0){
                    dismiss();
                }else{
                    showDialogFragment();
                }
            }

            @Override
            public void onStageLevelFinish(StageMessage message) {
                updateProgressInfo(BEGIN_ANALYZE_IMAGE);
            }

            @Override
            public void onError(String message) {

                // Update progress bar
                updateProgressInfo("");

                // Set the not found layout
                showDialogFragment();
            }

        }, bitmap);

        mSlyceRequest.execute();

        updateProgressInfo(BEGIN_SENDING_IMAGE);
    }

    private void updateProgressInfo(String progress) {

        Resources resources = getResources();

        switch (progress) {
            case BEGIN_SENDING_IMAGE:
                task = new UpdateProgressBarAsyncTask();
                task.execute();

                progressSendingImage.setVisibility(View.VISIBLE);
                sendDoneImage.setVisibility(View.INVISIBLE);
                progressAnalyzeImage.setVisibility(View.INVISIBLE);
                analyzeDoneImage.setVisibility(View.INVISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.image_analyse_in_process));
                analyzeImageText.setTextColor(resources.getColor(R.color.image_analyse_pre_process));

                break;

            case BEGIN_ANALYZE_IMAGE:
                task.cancel(true);

                progressSendingImage.setVisibility(View.INVISIBLE);
                sendDoneImage.setVisibility(View.VISIBLE);
                progressAnalyzeImage.setVisibility(View.VISIBLE);
                analyzeDoneImage.setVisibility(View.INVISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.image_analyse_post_process));
                analyzeImageText.setTextColor(resources.getColor(R.color.image_analyse_in_process));

                break;

            case FINISH_ANALYZE_IMAGE:

                horizontalProgressBar.setProgress(100);

                progressSendingImage.setVisibility(View.INVISIBLE);
                sendDoneImage.setVisibility(View.VISIBLE);
                progressAnalyzeImage.setVisibility(View.INVISIBLE);
                analyzeDoneImage.setVisibility(View.VISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.image_analyse_post_process));
                analyzeImageText.setTextColor(resources.getColor(R.color.image_analyse_post_process));

                break;

            default:

                if (task != null) {
                    task.cancel(true);
                }
                horizontalProgressBar.setProgress(0);
                progressSendingImage.setVisibility(View.INVISIBLE);
                sendDoneImage.setVisibility(View.INVISIBLE);
                progressAnalyzeImage.setVisibility(View.INVISIBLE);
                analyzeDoneImage.setVisibility(View.INVISIBLE);

                sendImageText.setTextColor(resources.getColor(R.color.image_analyse_pre_process));
                analyzeImageText.setTextColor(resources.getColor(R.color.image_analyse_pre_process));
        }
    }

    private class UpdateProgressBarAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int i = 0;
            int updateJump = 5;
            while (i <= 50) {
                if (!isCancelled()) {
                    i = i + updateJump;
                    horizontalProgressBar.setProgress(i);
                    SystemClock.sleep(UPLOAD_IMAGE_TOTAL_PROGRESS_TIME / (50 / updateJump));

                } else {
                    return null;
                }
            }
            return null;
        }
    }
    /* End */

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;
        private final WeakReference<ProgressBar> progressBarReference;
        private String data;

        public BitmapWorkerTask(ImageView imageView, ProgressBar progress) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            progressBarReference = new WeakReference<ProgressBar>(progress);
        }

        @Override
        protected void onPreExecute() {
            progressBarReference.get().setVisibility(View.VISIBLE);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            data = params[0];
            return BitmapLoader.decodeSampledBitmapFromResource(data, 200, 200);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }

                performSlyceProductsRequest(bitmap);

                progressBarReference.get().setVisibility(View.GONE);
            }
        }
    }


}
