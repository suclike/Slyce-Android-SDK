package com.android.slyce.activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.slyce.Slyce;
import com.android.slyce.camera.SlyceCamera;
import com.android.slyce.listeners.OnSlyceCameraFragmentListener;
import com.android.slyce.listeners.OnSlyceCameraListener;
import com.android.slyce.listeners.OnSlyceOpenListener;
import com.android.slyce.listeners.OnSlyceRequestListener;
import com.android.slyce.models.SlyceBarcode;
import com.android.slyce.utils.SlyceLog;
import com.android.slyce.utils.Utils;
import com.android.slycesdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnSlyceCameraFragmentListener} interface
 * to handle interaction events.
 * Use the {@link SlyceCameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SlyceCameraFragment extends Fragment implements OnSlyceCameraListener, OnClickListener{

    private static final String TAG = SlyceCameraFragment.class.getSimpleName();

    private static final int RESULT_LOAD_IMG = 1;

    // the fragment initialization parameters
    private static final String ARG_CLIENT_ID = "arg_client_id";
    private static final String ARG_OPTION_JSON = "arg_option_json";

    private String mClientID;
    private JSONObject mOptionsJson;

    private OnSlyceCameraFragmentListener mListener;

    /* Camera surface view */
    private SurfaceView mPreview;

    /* Views */
    private Button mCloseButton;
    private ImageButton mScanTipsButton;
    private ImageButton mGalleryButton;
    private CheckBox mFlashButton;
    private ImageButton mSnapButton;

    private ImageView mOnTapView;

    /* Slyce SDK object */
    private Slyce mSlyce;

    /* Slyce Camera object */
    private SlyceCamera mSlyceCamera;

    /* Notify the ImageProcessFragment on events */
    private OnImageProcessListener mImageProcessListener;

    public interface OnImageProcessListener{

        void onSnap(Bitmap bitmap);
        void onProgress(long progress, String message);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param clientID Parameter 1.
     * @param options  Parameter 2.
     * @return A new instance of fragment SlyceCameraFragment.
     */
    public static SlyceCameraFragment newInstance(String clientID, JSONObject options) {
        SlyceCameraFragment fragment = new SlyceCameraFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CLIENT_ID, clientID);

        if(options != null){
            args.putString(ARG_OPTION_JSON, options.toString());
        }

        fragment.setArguments(args);
        return fragment;
    }

    public SlyceCameraFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentArguments();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_slyce, container, false);

        // Initialize views
        initViews(root);

        // Create SlyceCamera object
        createSlyceCamera();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mSlyceCamera != null){
            mSlyceCamera.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mSlyceCamera != null){
            mSlyceCamera.stop();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSlyceCameraFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSlyceCameraFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCamera3DRecognition(JSONArray products) {
        mListener.onCameraFragment3DRecognition(products);
    }

    @Override
    public void onCameraBarcodeRecognition(SlyceBarcode barcode) {
        mListener.onCameraFragmentBarcodeRecognition(barcode);
    }

    @Override
    public void onCamera2DRecognition(String irId, String productInfo) {
        mListener.onCameraFragment2DRecognition(irId, productInfo);
    }

    @Override
    public void onCamera2DExtendedRecognition(JSONArray products) {
        mListener.onCameraFragment2DExtendedRecognition(products);
    }

    @Override
    public void onCameraSlyceProgress(long progress, String message, String id) {
        mListener.onCameraFragmentSlyceProgress(progress, message, id);
    }

    @Override
    public void onCameraStageLevelFinish(OnSlyceRequestListener.StageMessage message) {
        mListener.onCameraFragmentStageLevelFinish(message);
    }

    @Override
    public void onSlyceCameraError(String message) {
        mListener.onSlyceCameraFragmentError(message);
    }

    @Override
    public void onImageStartRequest(Bitmap bitmap) {
        mListener.onImageStartRequest(bitmap);
    }

    @Override
    public void onSnap(Bitmap bitmap) {
        mListener.onSnap(bitmap);
    }

    @Override
    public void onTap(float x, float y) {
        mListener.onTap(x, y);

        Utils.performAlphaAnimation(mOnTapView, x, y);
    }

    private void createSlyceCamera(){
        mSlyceCamera = new SlyceCamera(getActivity(), Slyce.get(), mPreview, mOptionsJson, this);
    }

    private void initViews(View view){
        mPreview = (SurfaceView) view.findViewById(R.id.preview);
        mCloseButton = (Button) view.findViewById(R.id.close_button);
        mScanTipsButton = (ImageButton) view.findViewById(R.id.scan_tips_button);
        mGalleryButton = (ImageButton) view.findViewById(R.id.gallery_button);
        mFlashButton = (CheckBox) view.findViewById(R.id.flash_button);
        mSnapButton = (ImageButton) view.findViewById(R.id.snap_button);

        mOnTapView = (ImageView) view.findViewById(R.id.on_tap_view);

        mCloseButton.setOnClickListener(this);
        mScanTipsButton.setOnClickListener(this);
        mGalleryButton.setOnClickListener(this);
        mFlashButton.setOnClickListener(this);
        mSnapButton.setOnClickListener(this);
    }

    private void getFragmentArguments(){
        if (getArguments() != null) {

            // Parameter 1. Set Client ID
            mClientID = getArguments().getString(ARG_CLIENT_ID);

            // Parameter 2. Set Options Json
            String options = getArguments().getString(ARG_OPTION_JSON);
            if(!TextUtils.isEmpty(options)){
                try {
                    mOptionsJson = new JSONObject(options);
                } catch (JSONException e) {
                    SlyceLog.i(TAG, "Failed to create options Json");
                }
            }

            // Parameter 3.
        }
    }

    private void close(){
        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onClick(View v) {

        int id = v.getId();

        if(id == R.id.close_button){

            close();

        }else if(id == R.id.scan_tips_button){

        }else if(id == R.id.gallery_button){

            Utils.loadImageFromGallery(this, RESULT_LOAD_IMG);

        }else if(id == R.id.flash_button){

            mSlyceCamera.turnFlash();

        }else if(id == R.id.snap_button){

            mSlyceCamera.snap();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        // When an Image is picked
        if (requestCode == RESULT_LOAD_IMG && resultCode == getActivity().RESULT_OK && null != data) {

            // Extract Image String
            String pickedImageString  = Utils.getImageDecodableString(data, getActivity().getApplicationContext());

            // Show ImageProcessFragment
            startImageProcessFragment(pickedImageString);

        } else {
            SlyceLog.i(TAG, "You haven't picked Image");
        }
    }

    private void startImageProcessFragment(String imageString){
        if(TextUtils.isEmpty(imageString)){
            SlyceLog.i(TAG, "Invalid Image picked");
            return;
        }

        ImageProcessFragment fragment = ImageProcessFragment.newInstance(imageString);
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.image_process_fragment_container, fragment);
        transaction.commitAllowingStateLoss();
    }
}