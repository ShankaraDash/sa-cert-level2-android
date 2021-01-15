package io.agora.api.example.examples.advanced;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import io.agora.api.example.R;
import io.agora.api.example.annotation.Example;
import io.agora.api.example.common.BaseFragment;
import io.agora.api.example.examples.basic.JoinChannelVideo;
import io.agora.api.example.utils.CommonUtil;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

import static io.agora.api.example.common.model.Examples.ADVANCED;
import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_FIT;
import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN;
import static io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15;
import static io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.STANDARD_BITRATE;
import static io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x360;

@Example(
        index = 12,
        group = ADVANCED,
        name = R.string.item_joinmultichannel,
        actionId = R.id.action_mainFragment_to_MultiChannel,
        tipsId = R.string.joinmultichannel
)
public class JoinMultipleChannel extends BaseFragment implements View.OnClickListener {
    private static final String TAG = JoinChannelVideo.class.getSimpleName();

    private FrameLayout fl_local, fl_remote, fl_remote2;
    private Button join;
    private EditText et_channel;
    private RtcEngineEx engine;
    private int myUid;
    private boolean joined = false;
    private String channel1;
    private String channel2;
    private RtcConnection rtcConnection2 = new RtcConnection();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_join_multi_channel, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        join = view.findViewById(R.id.btn_join);
        et_channel = view.findViewById(R.id.et_channel);
        view.findViewById(R.id.btn_join).setOnClickListener(this);
        fl_local = view.findViewById(R.id.fl_local);
        fl_remote = view.findViewById(R.id.fl_remote);
        fl_remote2 = view.findViewById(R.id.fl_remote2);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        // Check if the context is valid
        Context context = getContext();
        if (context == null)
        {
            return;
        }
        try
        {
            RtcEngineConfig config = new RtcEngineConfig();
            /**
             * The context of Android Activity
             */
            config.mContext = context.getApplicationContext();
            /**
             * The App ID issued to you by Agora. See <a href="https://docs.agora.io/en/Agora%20Platform/token#get-an-app-id"> How to get the App ID</a>
             */
            config.mAppId = getString(R.string.agora_app_id);
            /** Sets the channel profile of the Agora RtcEngine.
             CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile.
             Use this profile in one-on-one calls or group calls, where all users can talk freely.
             CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast
             channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams;
             an audience can only receive streams.*/
            config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
            /**
             * IRtcEngineEventHandler is an abstract class providing default implementation.
             * The SDK uses this class to report to the app on SDK runtime events.
             */
            config.mEventHandler = iRtcEngineEventHandler;
            config.mAudioScenario = Constants.AudioScenario.getValue(Constants.AudioScenario.HIGH_DEFINITION);
            engine = (RtcEngineEx) RtcEngine.create(config);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            getActivity().onBackPressed();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        /**leaveChannel and Destroy the RtcEngine instance*/
        if(engine != null)
        {
            engine.leaveChannel();
            engine.stopPreview();
        }
        handler.post(RtcEngine::destroy);
        engine = null;
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.btn_join)
        {
            if (!joined)
            {
                CommonUtil.hideInputBoard(getActivity(), et_channel);
                // call when join button hit
                channel1 = et_channel.getText().toString();
                channel2 = channel1 + "-2";
                // Check permission
                if (AndPermission.hasPermissions(this, Permission.Group.STORAGE, Permission.Group.MICROPHONE, Permission.Group.CAMERA))
                {
                    joinChannel(channel1);
                    return;
                }
                // Request permission
                AndPermission.with(this).runtime().permission(
                        Permission.Group.STORAGE,
                        Permission.Group.MICROPHONE,
                        Permission.Group.CAMERA
                ).onGranted(permissions ->
                {
                    // Permissions Granted
                    joinChannel(channel1);
                }).start();
            }
            else
            {
                joined = false;
                /**After joining a channel, the user must call the leaveChannel method to end the
                 * call before joining another channel. This method returns 0 if the user leaves the
                 * channel and releases all resources related to the call. This method call is
                 * asynchronous, and the user has not exited the channel when the method call returns.
                 * Once the user leaves the channel, the SDK triggers the onLeaveChannel callback.
                 * A successful leaveChannel method call triggers the following callbacks:
                 *      1:The local client: onLeaveChannel.
                 *      2:The remote client: onUserOffline, if the user leaving the channel is in the
                 *          Communication channel, or is a BROADCASTER in the Live Broadcast profile.
                 * @returns 0: Success.
                 *          < 0: Failure.
                 * PS:
                 *      1:If you call the destroy method immediately after calling the leaveChannel
                 *          method, the leaveChannel process interrupts, and the SDK does not trigger
                 *          the onLeaveChannel callback.
                 *      2:If you call the leaveChannel method during CDN live streaming, the SDK
                 *          triggers the removeInjectStreamUrl method.*/
                engine.leaveChannel();
                join.setText(getString(R.string.join));
            }
        }
    }

    private void joinChannel(String channelId)
    {
        // Check if the context is valid
        Context context = getContext();
        if (context == null)
        {
            return;
        }

        // Create render view by RtcEngine
        SurfaceView surfaceView = RtcEngine.CreateRendererView(context);
        // Local video is on the top
        surfaceView.setZOrderMediaOverlay(true);
        if(fl_local.getChildCount() > 0)
        {
            fl_local.removeAllViews();
        }
        // Add to the local container
        fl_local.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Setup local video to render your local camera preview
        engine.setupLocalVideo(new VideoCanvas(surfaceView, RENDER_MODE_HIDDEN, 0));
        // Set audio route to microPhone
        engine.setDefaultAudioRoutetoSpeakerphone(false);

        /** Sets the channel profile of the Agora RtcEngine.
         CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile.
         Use this profile in one-on-one calls or group calls, where all users can talk freely.
         CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast
         channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams;
         an audience can only receive streams.*/
        engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        /**In the demo, the default is to enter as the anchor.*/
        engine.setClientRole(IRtcEngineEventHandler.ClientRole.CLIENT_ROLE_BROADCASTER);
        // Enable video module
        engine.enableVideo();
        // start preview
        engine.startPreview();
        // Setup video encoding configs
        engine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VD_640x360,
                FRAME_RATE_FPS_15,
                STANDARD_BITRATE,
                ORIENTATION_MODE_ADAPTIVE
        ));

        /**Please configure accessToken in the string_config file.
         * A temporary token generated in Console. A temporary token is valid for 24 hours. For details, see
         *      https://docs.agora.io/en/Agora%20Platform/token?platform=All%20Platforms#get-a-temporary-token
         * A token generated at the server. This applies to scenarios with high-security requirements. For details, see
         *      https://docs.agora.io/en/cloud-recording/token_server_java?platform=Java*/
        String accessToken = getString(R.string.agora_access_token);
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(accessToken, "<#YOUR ACCESS TOKEN#>"))
        {
            accessToken = null;
        }
        /** Allows a user to join a channel.
         if you do not specify the uid, we will generate the uid for you*/
        int res = engine.joinChannel(accessToken, channelId, "Extra Optional Data", 0);
        if (res != 0 || !joinSecondChannel())
        {
            // Usually happens with invalid parameters
            // Error code description can be found at:
            // en: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
            // cn: https://docs.agora.io/cn/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
            showAlert(RtcEngine.getErrorDescription(Math.abs(res)));
            return;
        }
        // Prevent repeated entry
        join.setEnabled(false);
    }

    private boolean joinSecondChannel() {
        ChannelMediaOptions mediaOptions = new ChannelMediaOptions();
        mediaOptions.autoSubscribeAudio = true;
        mediaOptions.autoSubscribeVideo = true;
        int ret = engine.joinChannelEx(null, channel2, 0, mediaOptions, iRtcEngineEventHandler2, rtcConnection2);
        return (ret == 0);
    }


    private final IRtcEngineEventHandler iRtcEngineEventHandler2 = new IRtcEngineEventHandler() {
        /**
         * Occurs when the local user joins a specified channel.
         * The channel name assignment is based on channelName specified in the joinChannel method.
         * If the uid is not specified when joinChannel is called, the server automatically assigns a uid.
         *
         * @param channel Channel name
         * @param uid     User ID
         * @param elapsed Time elapsed (ms) from the user calling joinChannel until this callback is triggered
         */
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.i(TAG, String.format("channel2 onJoinChannelSuccess channel %s uid %d", channel2, uid));
            showLongToast(String.format("onJoinChannelSuccess channel %s uid %d", channel2, uid));
            myUid = uid;
            joined = true;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    join.setEnabled(true);
                    join.setText(getString(R.string.leave));
                }
            });
        }
        /**
         * Occurs when a remote user (Communication)/host (Live Broadcast) joins the channel.
         *
         * @param uid     ID of the user whose audio state changes.
         * @param elapsed Time delay (ms) from the local user calling joinChannel/setClientRole
         *                until this callback is triggered.
         */
        @Override
        public void onUserJoined(int uid, int elapsed) {
            Log.i(TAG, "channel2 onUserJoined->" + uid);
            showLongToast(String.format("user %d joined!", uid));
            /**Check if the context is correct*/
            Context context = getContext();
            if (context == null) {
                return;
            }
            handler.post(() ->
            {
                /**Display remote video stream*/
                SurfaceView surfaceView = null;
                if (fl_remote2.getChildCount() > 0) {
                    fl_remote2.removeAllViews();
                }
                // Create render view by RtcEngine
                surfaceView = RtcEngine.CreateRendererView(context);
                surfaceView.setZOrderMediaOverlay(true);
                // Add to the remote container
                fl_remote2.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                // Setup remote video to render
                engine.setupRemoteVideoEx(new VideoCanvas(surfaceView, RENDER_MODE_FIT, uid), rtcConnection2);
            });
        }

    };

    /**
     * IRtcEngineEventHandler is an abstract class providing default implementation.
     * The SDK uses this class to report to the app on SDK runtime events.
     */
    private final IRtcEngineEventHandler iRtcEngineEventHandler = new IRtcEngineEventHandler() {
        /**
         * Reports a warning during SDK runtime.
         * Warning code: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_warn_code.html
         */
        @Override
        public void onWarning(int warn) {
            Log.w(TAG, String.format("onWarning code %d message %s", warn, RtcEngine.getErrorDescription(warn)));
        }

        /**
         * Reports an error during SDK runtime.
         * Error code: https://docs.agora.io/en/Voice/API%20Reference/java/classio_1_1agora_1_1rtc_1_1_i_rtc_engine_event_handler_1_1_error_code.html
         */
        @Override
        public void onError(int err) {
            Log.e(TAG, String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)));
            showAlert(String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)));
        }

        /**
         * Occurs when a user leaves the channel.
         *
         * @param stats With this callback, the application retrieves the channel information,
         *              such as the call duration and statistics.
         */
        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            Log.i(TAG, String.format("local user %d leaveChannel!", myUid));
            showLongToast(String.format("local user %d leaveChannel!", myUid));
        }

        /**
         * Occurs when the local user joins a specified channel.
         * The channel name assignment is based on channelName specified in the joinChannel method.
         * If the uid is not specified when joinChannel is called, the server automatically assigns a uid.
         *
         * @param channel Channel name
         * @param uid     User ID
         * @param elapsed Time elapsed (ms) from the user calling joinChannel until this callback is triggered
         */
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.i(TAG, String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
            showLongToast(String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
            myUid = uid;
            joined = true;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    join.setEnabled(true);
                    join.setText(getString(R.string.leave));
                }
            });
        }

        /**
         * Since v2.9.0.
         * This callback indicates the state change of the remote audio stream.
         * PS: This callback does not work properly when the number of users (in the Communication profile) or
         * broadcasters (in the Live-broadcast profile) in the channel exceeds 17.
         *
         * @param uid     ID of the user whose audio state changes.
         * @param state   State of the remote audio
         *                REMOTE_AUDIO_STATE_STOPPED(0): The remote audio is in the default state, probably due
         *                to REMOTE_AUDIO_REASON_LOCAL_MUTED(3), REMOTE_AUDIO_REASON_REMOTE_MUTED(5),
         *                or REMOTE_AUDIO_REASON_REMOTE_OFFLINE(7).
         *                REMOTE_AUDIO_STATE_STARTING(1): The first remote audio packet is received.
         *                REMOTE_AUDIO_STATE_DECODING(2): The remote audio stream is decoded and plays normally,
         *                probably due to REMOTE_AUDIO_REASON_NETWORK_RECOVERY(2),
         *                REMOTE_AUDIO_REASON_LOCAL_UNMUTED(4) or REMOTE_AUDIO_REASON_REMOTE_UNMUTED(6).
         *                REMOTE_AUDIO_STATE_FROZEN(3): The remote audio is frozen, probably due to
         *                REMOTE_AUDIO_REASON_NETWORK_CONGESTION(1).
         *                REMOTE_AUDIO_STATE_FAILED(4): The remote audio fails to start, probably due to
         *                REMOTE_AUDIO_REASON_INTERNAL(0).
         * @param reason  The reason of the remote audio state change.
         *                REMOTE_AUDIO_REASON_INTERNAL(0): Internal reasons.
         *                REMOTE_AUDIO_REASON_NETWORK_CONGESTION(1): Network congestion.
         *                REMOTE_AUDIO_REASON_NETWORK_RECOVERY(2): Network recovery.
         *                REMOTE_AUDIO_REASON_LOCAL_MUTED(3): The local user stops receiving the remote audio
         *                stream or disables the audio module.
         *                REMOTE_AUDIO_REASON_LOCAL_UNMUTED(4): The local user resumes receiving the remote audio
         *                stream or enables the audio module.
         *                REMOTE_AUDIO_REASON_REMOTE_MUTED(5): The remote user stops sending the audio stream or
         *                disables the audio module.
         *                REMOTE_AUDIO_REASON_REMOTE_UNMUTED(6): The remote user resumes sending the audio stream
         *                or enables the audio module.
         *                REMOTE_AUDIO_REASON_REMOTE_OFFLINE(7): The remote user leaves the channel.
         * @param elapsed Time elapsed (ms) from the local user calling the joinChannel method
         *                until the SDK triggers this callback.
         */
        @Override
        public void onRemoteAudioStateChanged(int uid, IRtcEngineEventHandler.REMOTE_AUDIO_STATE state, IRtcEngineEventHandler.REMOTE_AUDIO_STATE_REASON reason, int elapsed)
        {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
            Log.i(TAG, "onRemoteAudioStateChanged->" + uid + ", state->" + state + ", reason->" + reason);
        }

        /**
         * Since v2.9.0.
         * Occurs when the remote video state changes.
         * PS: This callback does not work properly when the number of users (in the Communication
         * profile) or broadcasters (in the Live-broadcast profile) in the channel exceeds 17.
         *
         * @param uid     ID of the remote user whose video state changes.
         * @param state   State of the remote video:
         *                REMOTE_VIDEO_STATE_STOPPED(0): The remote video is in the default state, probably due
         *                to REMOTE_VIDEO_STATE_REASON_LOCAL_MUTED(3), REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED(5),
         *                or REMOTE_VIDEO_STATE_REASON_REMOTE_OFFLINE(7).
         *                REMOTE_VIDEO_STATE_STARTING(1): The first remote video packet is received.
         *                REMOTE_VIDEO_STATE_DECODING(2): The remote video stream is decoded and plays normally,
         *                probably due to REMOTE_VIDEO_STATE_REASON_NETWORK_RECOVERY (2),
         *                REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED(4), REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED(6),
         *                or REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK_RECOVERY(9).
         *                REMOTE_VIDEO_STATE_FROZEN(3): The remote video is frozen, probably due to
         *                REMOTE_VIDEO_STATE_REASON_NETWORK_CONGESTION(1) or REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK(8).
         *                REMOTE_VIDEO_STATE_FAILED(4): The remote video fails to start, probably due to
         *                REMOTE_VIDEO_STATE_REASON_INTERNAL(0).
         * @param reason  The reason of the remote video state change:
         *                REMOTE_VIDEO_STATE_REASON_INTERNAL(0): Internal reasons.
         *                REMOTE_VIDEO_STATE_REASON_NETWORK_CONGESTION(1): Network congestion.
         *                REMOTE_VIDEO_STATE_REASON_NETWORK_RECOVERY(2): Network recovery.
         *                REMOTE_VIDEO_STATE_REASON_LOCAL_MUTED(3): The local user stops receiving the remote
         *                video stream or disables the video module.
         *                REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED(4): The local user resumes receiving the remote
         *                video stream or enables the video module.
         *                REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED(5): The remote user stops sending the video
         *                stream or disables the video module.
         *                REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED(6): The remote user resumes sending the video
         *                stream or enables the video module.
         *                REMOTE_VIDEO_STATE_REASON_REMOTE_OFFLINE(7): The remote user leaves the channel.
         *                REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK(8): The remote media stream falls back to the
         *                audio-only stream due to poor network conditions.
         *                REMOTE_VIDEO_STATE_REASON_AUDIO_FALLBACK_RECOVERY(9): The remote media stream switches
         *                back to the video stream after the network conditions improve.
         * @param elapsed Time elapsed (ms) from the local user calling the joinChannel method until
         *                the SDK triggers this callback.
         */
        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed);
            Log.i(TAG, "onRemoteVideoStateChanged->" + uid + ", state->" + state + ", reason->" + reason);
        }

        /**
         * Occurs when a remote user (Communication)/host (Live Broadcast) joins the channel.
         *
         * @param uid     ID of the user whose audio state changes.
         * @param elapsed Time delay (ms) from the local user calling joinChannel/setClientRole
         *                until this callback is triggered.
         */
        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.i(TAG, "onUserJoined->" + uid);
            showLongToast(String.format("user %d joined!", uid));
            /**Check if the context is correct*/
            Context context = getContext();
            if (context == null) {
                return;
            }
            handler.post(() ->
            {
                /**Display remote video stream*/
                SurfaceView surfaceView = null;
                if (fl_remote.getChildCount() > 0) {
                    fl_remote.removeAllViews();
                }
                // Create render view by RtcEngine
                surfaceView = RtcEngine.CreateRendererView(context);
                surfaceView.setZOrderMediaOverlay(true);
                // Add to the remote container
                fl_remote.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                // Setup remote video to render
                engine.setupRemoteVideo(new VideoCanvas(surfaceView, RENDER_MODE_FIT, uid));
            });
        }

        /**
         * Occurs when a remote user (Communication)/host (Live Broadcast) leaves the channel.
         *
         * @param uid    ID of the user whose audio state changes.
         * @param reason Reason why the user goes offline:
         *               USER_OFFLINE_QUIT(0): The user left the current channel.
         *               USER_OFFLINE_DROPPED(1): The SDK timed out and the user dropped offline because no data
         *               packet was received within a certain period of time. If a user quits the
         *               call and the message is not passed to the SDK (due to an unreliable channel),
         *               the SDK assumes the user dropped offline.
         *               USER_OFFLINE_BECOME_AUDIENCE(2): (Live broadcast only.) The client role switched from
         *               the host to the audience.
         */
        @Override
        public void onUserOffline(int uid, int reason) {
            Log.i(TAG, String.format("user %d offline! reason:%d", uid, reason));
            showLongToast(String.format("user %d offline! reason:%d", uid, reason));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    /**Clear render view
                     Note: The video will stay at its last frame, to completely remove it you will need to
                     remove the SurfaceView from its parent*/
                    engine.setupRemoteVideo(new VideoCanvas(null, RENDER_MODE_HIDDEN, uid));
                }
            });
        }
    };
}
