package com.laka.shoppingchat.mvp.nim.session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import com.laka.androidlib.util.ApplicationUtils;
import com.laka.androidlib.widget.dialog.CommonConfirmDialog;
import com.laka.shoppingchat.R;
import com.laka.shoppingchat.mvp.chat.ChatModuleNavigator;
import com.laka.shoppingchat.mvp.nim.activity.TeamListActivity;
import com.laka.shoppingchat.mvp.nim.activity.UserInfoActivity;
import com.laka.shoppingchat.mvp.nim.session.activity.MessageHistoryActivity;
import com.laka.shoppingchat.mvp.nim.session.activity.MessageInfoActivity;
import com.laka.shoppingchat.mvp.nim.session.extension.StickerAttachment;
import com.laka.shoppingchat.mvp.nim.session.search.SearchMessageActivity;
import com.laka.shoppingchat.mvp.nim.session.viewholder.MsgViewHolderDefCustom;
import com.laka.shoppingchat.mvp.nim.session.viewholder.MsgViewHolderTip;
import com.laka.shoppingchat.mvp.user.utils.UserUtils;
import com.laka.shoppingchat.mvp.user.UserCenterModuleNavigator;
import com.laka.shoppingchat.mvp.wallet.MyWalletNavigator;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.api.model.recent.RecentCustomization;
import com.netease.nim.uikit.api.model.session.SessionCustomization;
import com.netease.nim.uikit.api.model.session.SessionEventListener;
import com.netease.nim.uikit.api.wrapper.NimMessageRevokeObserver;
import com.netease.nim.uikit.business.ChatBusinessNavigator;
import com.netease.nim.uikit.business.ChatRouteManager;
import com.netease.nim.uikit.business.contact.core.item.ItemTypes;
import com.netease.nim.uikit.business.session.actions.BaseAction;
import com.netease.nim.uikit.business.session.attachment.CustomAttachParser;
import com.netease.nim.uikit.business.session.attachment.CustomAttachment;
import com.netease.nim.uikit.business.session.attachment.RedPackageAttachment;
import com.netease.nim.uikit.business.session.attachment.RobRedPackageAttachment;
import com.netease.nim.uikit.business.session.constant.RedPackageConstant;
import com.netease.nim.uikit.business.session.helper.MessageListPanelHelper;
import com.netease.nim.uikit.business.session.module.MsgForwardFilter;
import com.netease.nim.uikit.business.session.module.MsgRevokeFilter;
import com.netease.nim.uikit.business.session.viewholder.MsgViewHolderCustomNotification;
import com.netease.nim.uikit.business.session.viewholder.MsgViewHolderRedPackage;
import com.netease.nim.uikit.common.ui.dialog.CustomAlertDialog;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.uikit.common.ui.popupmenu.NIMPopupMenu;
import com.netease.nim.uikit.common.ui.popupmenu.PopupMenuItem;
import com.netease.nim.uikit.impl.cache.TeamDataCache;
import com.netease.nim.uikit.impl.customization.DefaultRecentCustomization;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachment;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.LocalAntiSpamResult;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.robot.model.RobotAttachment;
import com.netease.nimlib.sdk.team.constant.TeamTypeEnum;
import com.netease.nimlib.sdk.team.model.Team;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * UIKit自定义消息界面用法展示类
 */
public class SessionHelper {

    private static final int ACTION_HISTORY_QUERY = 0;

    private static final int ACTION_SEARCH_MESSAGE = 1;

    private static final int ACTION_CLEAR_MESSAGE = 2;

    private static final int ACTION_CLEAR_P2P_MESSAGE = 3;

    private static SessionCustomization p2pCustomization;
    private static SessionCustomization teamInfoClickCustomization;

    private static SessionCustomization normalTeamCustomization; //普通群

    private static SessionCustomization advancedTeamCustomization; //高级群

    private static SessionCustomization myP2pCustomization;

    private static SessionCustomization robotCustomization;

    private static RecentCustomization recentCustomization;

    private static NIMPopupMenu popupMenu;

    private static List<PopupMenuItem> menuItemList;

    public static final boolean USE_LOCAL_ANTISPAM = true;


    public static void init() {
        // 注册自定义消息附件解析器
        NIMClient.getService(MsgService.class).registerCustomAttachmentParser(new CustomAttachParser());
        // 注册各种扩展消息类型的显示ViewHolder
        registerViewHolders();
        // 设置会话中点击事件响应处理
        setSessionListener();
        // 注册消息转发过滤器
        registerMsgForwardFilter();
        // 注册消息撤回过滤器
        registerMsgRevokeFilter();
        // 注册消息撤回监听器
        registerMsgRevokeObserver();
        NimUIKit.setCommonP2PSessionCustomization(getP2pCustomization());
        NimUIKit.setCommonTeamInfoClickCustomization(getTeamInfoClickCustomization());
        NimUIKit.setCommonTeamSessionCustomization(getTeamCustomization(null));
        NimUIKit.setRecentCustomization(getRecentCustomization());

        initJumpAction();
    }

    private static void initJumpAction() {
        ChatRouteManager.put(ChatRouteManager.ROUTE_WALLET_ACTIVITY, (context, params) -> MyWalletNavigator.INSTANCE.startMyWalletActivity(context));
        ChatRouteManager.put(ChatRouteManager.ROUTE_REDPACKAGE_RECORD_ACTIVITY, (context, params) -> MyWalletNavigator.INSTANCE.startRedPackageListActivity(context));
        ChatRouteManager.put(ChatRouteManager.ROUTE_SEND_RED_PACKAGE_ACTIVITY, (context, params) -> {
            if (UserUtils.getPayPasswordStatus().equals("0")) {//未设置支付密码
                CommonConfirmDialog confirmDialog = new CommonConfirmDialog(context);
                confirmDialog.setDefaultTitleTxt("您未设置支付密码，前往设置？");
                confirmDialog.show();
                confirmDialog.setOnClickSureListener(view -> {
                    UserCenterModuleNavigator.INSTANCE.startOperationPayPsdActivity(context);
                });
            } else {
                SessionTypeEnum type = (SessionTypeEnum) params.get(RedPackageConstant.KEY_SESSION_TYPE);
                String account = (String) params.get(RedPackageConstant.KEY_USER_ACCOUNT);
                ChatBusinessNavigator.startSendRedPackageActivity((Activity) context, type, account);
            }
        });
        ChatRouteManager.put(ChatRouteManager.ROUTE_TEAM_QRCODE_ACTIVITY, (context, params) -> {
            //群二维码
            String account = (String) params.get("account");
            ChatModuleNavigator.INSTANCE.startChatTeamQrCodeActivity(context, account);
        });

        ChatRouteManager.put(ChatRouteManager.ROUTE_USER_INFO_ACTIVITY, (context, params) -> {
            //用户信息
            String account = (String) params.get("account");
            ChatModuleNavigator.INSTANCE.startUserInfoActivity(context, account);
        });
        ChatRouteManager.put(ChatRouteManager.ROUTE_WALLET_RECHARGE_ACTIVITY, (context, params) -> {
            MyWalletNavigator.INSTANCE.startRechargeActivityForResult((Activity) context, 1001);
        });
        ChatRouteManager.put(ChatRouteManager.ROUTE_UPDATE_PASSWORD_ACTIVITY,(context, params) -> {
            UserCenterModuleNavigator.INSTANCE.startOperationPayPsdActivity(context);
        });
    }

    public static void startP2PSession(Context context, String account) {
        startP2PSession(context, account, null);
    }

    public static void startP2PSession(Context context, String account, IMMessage anchor) {
        if (!UserUtils.getImAccount().equals(account)) {
            if (NimUIKit.getRobotInfoProvider().getRobotByAccount(account) != null) {
                NimUIKit.startChatting(context, account, SessionTypeEnum.P2P, getRobotCustomization(), anchor);
            } else {
                NimUIKit.startP2PSession(context, account, anchor);
            }
        } else {
            NimUIKit.startChatting(context, account, SessionTypeEnum.P2P, getMyP2pCustomization(), anchor);
        }
    }

    public static void startTeamSession(Context context, String tid) {
        startTeamSession(context, tid, null);
    }

    public static void startTeamSession(Context context, String tid, IMMessage anchor) {
        NimUIKit.startTeamSession(context, tid, getTeamCustomization(tid), anchor);
    }

    // 打开群聊界面(用于 UIKIT 中部分界面跳转回到指定的页面)
    public static void startTeamSession(Context context, String tid, Class<? extends Activity> backToClass,
                                        IMMessage anchor) {
        NimUIKit.startChatting(context, tid, SessionTypeEnum.Team, getTeamCustomization(tid), backToClass, anchor);
    }

    // 定制化单聊界面。如果使用默认界面，返回null即可
    private static SessionCustomization getP2pCustomization() {
        if (p2pCustomization == null) {
            p2pCustomization = new SessionCustomization() {

                // 由于需要Activity Result， 所以重载该函数。
                @Override
                public void onActivityResult(final Activity activity, int requestCode, int resultCode, Intent data) {
                    super.onActivityResult(activity, requestCode, resultCode, data);

                }

                @Override
                public boolean isAllowSendMessage(IMMessage message) {
                    return checkLocalAntiSpam(message);
                }

                @Override
                public MsgAttachment createStickerAttachment(String category, String item) {
                    return new StickerAttachment(category, item);
                }
            };
            // 背景
            //            p2pCustomization.backgroundColor = Color.BLUE;
            //            p2pCustomization.backgroundUri = "file:///android_asset/xx/bk.jpg";
            //            p2pCustomization.backgroundUri = "file:///sdcard/Pictures/bk.png";
            //            p2pCustomization.backgroundUri = "android.resource://com.netease.nim.demo/drawable/bk"
            // 定制加号点开后可以包含的操作， 默认已经有图片，视频等消息了
//            ArrayList<BaseAction> actions = new ArrayList<>();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//                actions.add(new AVChatAction(AVChatType.AUDIO));
//                actions.add(new AVChatAction(AVChatType.VIDEO));
//            }
//            actions.add(new RTSAction());
//            actions.add(new SnapChatAction());
//            actions.add(new GuessAction());
//            actions.add(new FileAction());
//            actions.add(new TipAction());
//            if (NIMRedPacketClient.isEnable()) {
//                actions.add(new RedPacketAction());
//            }
//            p2pCustomization.actions = actions;
            p2pCustomization.withSticker = true;
            // 定制ActionBar右边的按钮，可以加多个
            ArrayList<SessionCustomization.OptionsButton> buttons = new ArrayList<>();
            SessionCustomization.OptionsButton cloudMsgButton = new SessionCustomization.OptionsButton() {

                @Override
                public void onClick(Context context, View view, String sessionId) {
                    initPopuptWindow(context, view, sessionId, SessionTypeEnum.P2P);
                }
            };
//            cloudMsgButton.iconId = R.drawable.nim_ic_messge_history;
            SessionCustomization.OptionsButton infoButton = new SessionCustomization.OptionsButton() {

                @Override
                public void onClick(Context context, View view, String sessionId) {
                    MessageInfoActivity.startActivity(context, sessionId, getTeamInfoClickCustomization()); //打开聊天信息
                }
            };
            SessionCustomization.OptionsButton infoButton1 = new SessionCustomization.OptionsButton() {

                @Override
                public void onClick(Context context, View view, String sessionId) {
                    TeamListActivity.start(context, ItemTypes.TEAMS.ADVANCED_TEAM, true);
                }
            };
            buttons.add(infoButton);
            buttons.add(infoButton1);
            p2pCustomization.buttons = buttons;
            return p2pCustomization;
        }
        return null;
    }

    private static SessionCustomization getTeamInfoClickCustomization() {
        if (teamInfoClickCustomization == null) {
            teamInfoClickCustomization = new SessionCustomization() {
                @Override
                public void onActivityResult(final Activity activity, int requestCode, int resultCode, Intent data) {
                    super.onActivityResult(activity, requestCode, resultCode, data);

                }

                @Override
                public boolean isAllowSendMessage(IMMessage message) {
                    return checkLocalAntiSpam(message);
                }

                @Override
                public MsgAttachment createStickerAttachment(String category, String item) {
                    return new StickerAttachment(category, item);
                }
            };
            teamInfoClickCustomization.withSticker = true;
            // 定制ActionBar右边的按钮，可以加多个
            ArrayList<SessionCustomization.OptionsButton> buttons = new ArrayList<>();
            SessionCustomization.OptionsButton infoButton = new SessionCustomization.OptionsButton() {

                @Override
                public void onClick(Context context, View view, String sessionId) {
                    UserInfoActivity.start(context, sessionId);
                }
            };
            buttons.add(infoButton);
            teamInfoClickCustomization.buttons = buttons;

        }
        return teamInfoClickCustomization;
    }

    private static SessionCustomization getMyP2pCustomization() {
//        if (myP2pCustomization == null) {
//            myP2pCustomization = new SessionCustomization() {
//
//                // 由于需要Activity Result， 所以重载该函数。
//                @Override
//                public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
//                    if (requestCode == TeamRequestCode.REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//                        String result = data.getStringExtra(TeamExtras.RESULT_EXTRA_REASON);
//                        if (result == null) {
//                            return;
//                        }
//                        if (result.equals(TeamExtras.RESULT_EXTRA_REASON_CREATE)) {
//                            String tid = data.getStringExtra(TeamExtras.RESULT_EXTRA_DATA);
//                            if (TextUtils.isEmpty(tid)) {
//                                return;
//                            }
//                            startTeamSession(activity, tid);
//                            activity.finish();
//                        }
//                    }
//                }
//
//                @Override
//                public boolean isAllowSendMessage(IMMessage message) {
//                    return checkLocalAntiSpam(message);
//                }
//
//                @Override
//                public MsgAttachment createStickerAttachment(String category, String item) {
//                    return new StickerAttachment(category, item);
//                }
//            };
//            // 定制加号点开后可以包含的操作， 默认已经有图片，视频等消息了
//            ArrayList<BaseAction> actions = new ArrayList<>();
//            actions.add(new SnapChatAction());
//            actions.add(new GuessAction());
//            actions.add(new FileAction());
//            myP2pCustomization.actions = actions;
//            myP2pCustomization.withSticker = true;
//            // 定制ActionBar右边的按钮，可以加多个
//            ArrayList<SessionCustomization.OptionsButton> buttons = new ArrayList<>();
//            SessionCustomization.OptionsButton cloudMsgButton = new SessionCustomization.OptionsButton() {
//
//                @Override
//                public void onClick(Context context, View view, String sessionId) {
//                    initPopuptWindow(context, view, sessionId, SessionTypeEnum.P2P);
//                }
//            };
//            cloudMsgButton.iconId = R.drawable.nim_ic_messge_history;
//            buttons.add(cloudMsgButton);
//            myP2pCustomization.buttons = buttons;
//        }
//        return myP2pCustomization;
        return null;
    }

    private static boolean checkLocalAntiSpam(IMMessage message) {
        if (!USE_LOCAL_ANTISPAM) {
            return true;
        }
        LocalAntiSpamResult result = NIMClient.getService(MsgService.class).checkLocalAntiSpam(message.getContent(),
                "**");
        int operator = result == null ? 0 : result.getOperator();
        switch (operator) {
            case 1: // 替换，允许发送
                message.setContent(result.getContent());
                return true;
            case 2: // 拦截，不允许发送
                return false;
            case 3: // 允许发送，交给服务器
                message.setClientAntiSpam(true);
                return true;
            case 0:
            default:
                break;
        }
        return true;
    }

    private static SessionCustomization getRobotCustomization() {
//        if (robotCustomization == null) {
//            robotCustomization = new SessionCustomization() {
//
//                // 由于需要Activity Result， 所以重载该函数。
//                @Override
//                public void onActivityResult(final Activity activity, int requestCode, int resultCode, Intent data) {
//                    super.onActivityResult(activity, requestCode, resultCode, data);
//
//                }
//
//                @Override
//                public MsgAttachment createStickerAttachment(String category, String item) {
//                    return null;
//                }
//            };
//            // 定制ActionBar右边的按钮，可以加多个
//            ArrayList<SessionCustomization.OptionsButton> buttons = new ArrayList<>();
//            SessionCustomization.OptionsButton cloudMsgButton = new SessionCustomization.OptionsButton() {
//
//                @Override
//                public void onClick(Context context, View view, String sessionId) {
//                    initPopuptWindow(context, view, sessionId, SessionTypeEnum.P2P);
//                }
//            };
//            cloudMsgButton.iconId = R.mipmap.nim_ic_messge_history;
//            SessionCustomization.OptionsButton infoButton = new SessionCustomization.OptionsButton() {
//
//                @Override
//                public void onClick(Context context, View view, String sessionId) {
////                    RobotProfileActivity.start(context, sessionId); //打开聊天信息
//                }
//            };
////            infoButton.iconId = R.mipmap.nim_ic_actionbar_robot_info;
//            buttons.add(cloudMsgButton);
//            buttons.add(infoButton);
//            robotCustomization.buttons = buttons;
//        }
        return null;
    }

    private static RecentCustomization getRecentCustomization() {
        if (recentCustomization == null) {
            recentCustomization = new DefaultRecentCustomization() {

                @Override
                public String getDefaultDigest(RecentContact recent) {
                    switch (recent.getMsgType()) {
//                        case avchat:
//                            MsgAttachment attachment = recent.getAttachment();
//                            AVChatAttachment avchat = (AVChatAttachment) attachment;
//                            if (avchat.getState() == AVChatRecordState.Missed && !recent.getFromAccount().equals(
//                                    NimUIKit.getAccount())) {
//                                // 未接通话请求
//                                StringBuilder sb = new StringBuilder("[未接");
//                                if (avchat.getType() == AVChatType.VIDEO) {
//                                    sb.append("视频电话]");
//                                } else {
//                                    sb.append("音频电话]");
//                                }
//                                return sb.toString();
//                            } else if (avchat.getState() == AVChatRecordState.Success) {
//                                StringBuilder sb = new StringBuilder();
//                                if (avchat.getType() == AVChatType.VIDEO) {
//                                    sb.append("[视频电话]: ");
//                                } else {
//                                    sb.append("[音频电话]: ");
//                                }
//                                sb.append(TimeUtil.secToTime(avchat.getDuration()));
//                                return sb.toString();
//                            } else {
//                                if (avchat.getType() == AVChatType.VIDEO) {
//                                    return ("[视频电话]");
//                                } else {
//                                    return ("[音频电话]");
//                                }
//                            }
                    }
                    return super.getDefaultDigest(recent);
                }
            };
        }
        return recentCustomization;
    }

//    private static SessionCustomization sUserInfoCustomzation;
//
//    private static SessionCustomization getUserInfoSustomzation() {
//        ArrayList<SessionCustomization.OptionsButton> buttons = new ArrayList<>();
//        SessionCustomization.OptionsButton infoButton = new SessionCustomization.OptionsButton() {
//
//            @Override
//            public void onClick(Context context, View view, String sessionId) {
//                UserInfoActivity.start(context, sessionId, false);
//            }
//        };
//        buttons.add(infoButton);
//        sUserInfoCustomzation.buttons = buttons;
//        return sUserInfoCustomzation;
//    }

    private static SessionCustomization getTeamCustomization(String tid) {
        if (normalTeamCustomization == null) {
            // 定制加号点开后可以包含的操作， 默认已经有图片，视频等消息了
            ArrayList<BaseAction> actions = new ArrayList<>();
//            actions.add(new GuessAction());
//            actions.add(new FileAction());
//            actions.add(new TipAction());
            SessionTeamCustomization.SessionTeamCustomListener listener = new SessionTeamCustomization.SessionTeamCustomListener() {

                @Override
                public void initPopupWindow(Context context, View view, String sessionId,
                                            SessionTypeEnum sessionTypeEnum) {
                    initPopuptWindow(context, view, sessionId, sessionTypeEnum);
                }

                @Override
                public void onSelectedAccountsResult(ArrayList<String> selectedAccounts) {
//                    avChatAction.onSelectedAccountsResult(selectedAccounts);
                }

                @Override
                public void onSelectedAccountFail() {
//                    avChatAction.onSelectedAccountFail();
                }
            };
            normalTeamCustomization = new SessionTeamCustomization(listener) {

                @Override
                public boolean isAllowSendMessage(IMMessage message) {
                    return checkLocalAntiSpam(message);
                }
            };
            normalTeamCustomization.actions = actions;
        }
        if (advancedTeamCustomization == null) {
            // 定制加号点开后可以包含的操作， 默认已经有图片，视频等消息了
//            final TeamAVChatAction avChatAction = new TeamAVChatAction(AVChatType.VIDEO);
//            TeamAVChatProfile.sharedInstance().registerObserver(true);
            ArrayList<BaseAction> actions = new ArrayList<>();
//            actions.add(avChatAction);
//            actions.add(new GuessAction());
//            actions.add(new FileAction());
//            actions.add(new AckMessageAction());
//            if (NIMRedPacketClient.isEnable()) {
//                actions.add(new RedPacketAction());
//            }
//            actions.add(new TipAction());
            SessionTeamCustomization.SessionTeamCustomListener listener = new SessionTeamCustomization.SessionTeamCustomListener() {

                @Override
                public void initPopupWindow(Context context, View view, String sessionId,
                                            SessionTypeEnum sessionTypeEnum) {
                    initPopuptWindow(context, view, sessionId, sessionTypeEnum);
                }


                @Override
                public void onSelectedAccountsResult(ArrayList<String> selectedAccounts) {
//                    avChatAction.onSelectedAccountsResult(selectedAccounts);
                }

                @Override
                public void onSelectedAccountFail() {
//                    avChatAction.onSelectedAccountFail();
                }
            };
            advancedTeamCustomization = new SessionTeamCustomization(listener) {

                @Override
                public boolean isAllowSendMessage(IMMessage message) {
                    return checkLocalAntiSpam(message);
                }
            };
            advancedTeamCustomization.actions = actions;
        }
        if (TextUtils.isEmpty(tid)) {
            return normalTeamCustomization;
        } else {
            Team team = TeamDataCache.getInstance().getTeamById(tid);
            if (team != null && team.getType() == TeamTypeEnum.Advanced) {
                return advancedTeamCustomization;
            }
        }
        return normalTeamCustomization;
    }

    private static void registerViewHolders() {
        NimUIKit.registerMsgItemViewHolder(CustomAttachment.class, MsgViewHolderDefCustom.class);
        NimUIKit.registerMsgItemViewHolder(RedPackageAttachment.class, MsgViewHolderRedPackage.class);
        NimUIKit.registerMsgItemViewHolder(RobRedPackageAttachment.class, MsgViewHolderCustomNotification.class);
        NimUIKit.registerTipMsgViewHolder(MsgViewHolderTip.class);
//        NimUIKit.registerMsgItemViewHolder(FileAttachment.class, MsgViewHolderFile.class);
//        NimUIKit.registerMsgItemViewHolder(StickerAttachment.class, MsgViewHolderSticker.class);
//        NimUIKit.registerMsgItemViewHolder(GuessAttachment.class, MsgViewHolderGuess.class);
//        registerRedPacketViewHolder();
    }

    private static void registerRedPacketViewHolder() {
//        if (NIMRedPacketClient.isEnable()) {
//            NimUIKit.registerMsgItemViewHolder(RedPacketAttachment.class, MsgViewHolderRedPacket.class);
//            NimUIKit.registerMsgItemViewHolder(RedPacketOpenedAttachment.class, MsgViewHolderOpenRedPacket.class);
//        } else {
//            NimUIKit.registerMsgItemViewHolder(RedPacketAttachment.class, MsgViewHolderUnknown.class);
//            NimUIKit.registerMsgItemViewHolder(RedPacketOpenedAttachment.class, MsgViewHolderUnknown.class);
//        }
    }

    private static void setSessionListener() {
        SessionEventListener listener = new SessionEventListener() {

            @Override
            public void onAvatarClicked(Context context, IMMessage message) {
                // 一般用于打开用户资料页面
//                if (message.getMsgType() == MsgTypeEnum.robot && message.getDirect() == MsgDirectionEnum.In) {
//                    RobotAttachment attachment = (RobotAttachment) message.getAttachment();
//                    if (attachment.isRobotSend()) {
////                        RobotProfileActivity.start(context, attachment.getFromRobotAccount());
//                        return;
//                    }
//                }

//                if (message.getDirect() == MsgDirectionEnum.Out) {
//                    return;
//                }
                UserInfoActivity.start(context, message.getFromAccount());
            }

            @Override
            public void onAvatarLongClicked(Context context, IMMessage message) {
                // 一般用于群组@功能，或者弹出菜单，做拉黑，加好友等功能
            }

            @Override
            public void onAckMsgClicked(Context context, IMMessage message) {
                // 已读回执事件处理，用于群组的已读回执事件的响应，弹出消息已读详情
//                AckMsgInfoActivity.start(context, message);
            }
        };
        NimUIKit.setSessionListener(listener);
    }


    /**
     * 消息转发过滤器
     */
    private static void registerMsgForwardFilter() {
        NimUIKit.setMsgForwardFilter(new MsgForwardFilter() {

            @Override
            public boolean shouldIgnore(IMMessage message) {
                if (message.getMsgType() == MsgTypeEnum.custom && message.getAttachment() != null) {
                    // 白板消息和阅后即焚消息，红包消息 不允许转发
                    return true;
                } else if (message.getMsgType() == MsgTypeEnum.robot && message.getAttachment() != null &&
                        ((RobotAttachment) message.getAttachment()).isRobotSend()) {
                    return true; // 如果是机器人发送的消息 不支持转发
                }
                return false;
            }
        });
    }

    /**
     * 消息撤回过滤器
     */
    private static void registerMsgRevokeFilter() {
        NimUIKit.setMsgRevokeFilter(new MsgRevokeFilter() {

            @Override
            public boolean shouldIgnore(IMMessage message) {
                if (message.getAttachment() != null) {
                    // 视频通话消息和白板消息，红包消息 不允许撤回
                    return true;
                } else if (UserUtils.getImAccount().equals(message.getSessionId())) {
                    // 发给我的电脑 不允许撤回
                    return true;
                }
                return false;
            }
        });
    }

    private static void registerMsgRevokeObserver() {
        NIMClient.getService(MsgServiceObserve.class).observeRevokeMessage(new NimMessageRevokeObserver(), true);
    }


    private static void initPopuptWindow(Context context, View view, String sessionId,
                                         SessionTypeEnum sessionTypeEnum) {
        if (popupMenu == null) {
            menuItemList = new ArrayList<>();
            popupMenu = new NIMPopupMenu(context, menuItemList, listener);
        }
        menuItemList.clear();
        menuItemList.addAll(getMoreMenuItems(context, sessionId, sessionTypeEnum));
        popupMenu.notifyData();
        popupMenu.show(view);
    }

    private static NIMPopupMenu.MenuItemClickListener listener = new NIMPopupMenu.MenuItemClickListener() {

        @Override
        public void onItemClick(final PopupMenuItem item) {
            switch (item.getTag()) {
                case ACTION_HISTORY_QUERY:
                    MessageHistoryActivity.start(item.getContext(), item.getSessionId(),
                            item.getSessionTypeEnum()); // 漫游消息查询
                    break;
                case ACTION_SEARCH_MESSAGE:
                    SearchMessageActivity.start(item.getContext(), item.getSessionId(), item.getSessionTypeEnum());
                    break;
                case ACTION_CLEAR_MESSAGE:
                    EasyAlertDialogHelper.createOkCancelDiolag(item.getContext(), null, "确定要清空吗？", true,
                            new EasyAlertDialogHelper.OnDialogActionListener() {

                                @Override
                                public void doCancelAction() {
                                }

                                @Override
                                public void doOkAction() {
                                    NIMClient.getService(MsgService.class)
                                            .clearChattingHistory(
                                                    item.getSessionId(),
                                                    item.getSessionTypeEnum());
                                    MessageListPanelHelper.getInstance()
                                            .notifyClearMessages(
                                                    item.getSessionId());
                                }
                            }).show();
                    break;
                case ACTION_CLEAR_P2P_MESSAGE:
                    String title = item.getContext().getString(R.string.message_p2p_clear_tips);
                    CustomAlertDialog alertDialog = new CustomAlertDialog(item.getContext());
                    alertDialog.setTitle(title);
                    alertDialog.addItem("确定", new CustomAlertDialog.onSeparateItemClickListener() {

                        @Override
                        public void onClick() {
                            NIMClient.getService(MsgService.class).clearServerHistory(item.getSessionId(),
                                    item.getSessionTypeEnum());
                            MessageListPanelHelper.getInstance().notifyClearMessages(item.getSessionId());
                        }
                    });
                    String itemText = item.getContext().getString(R.string.sure_keep_roam);
                    alertDialog.addItem(itemText, new CustomAlertDialog.onSeparateItemClickListener() {

                        @Override
                        public void onClick() {
                            NIMClient.getService(MsgService.class).clearServerHistory(item.getSessionId(),
                                    item.getSessionTypeEnum(), false);
                            MessageListPanelHelper.getInstance().notifyClearMessages(item.getSessionId());
                        }
                    });
                    alertDialog.addItem("取消", new CustomAlertDialog.onSeparateItemClickListener() {

                        @Override
                        public void onClick() {
                        }
                    });
                    alertDialog.show();
                    break;
            }
        }
    };

    private static List<PopupMenuItem> getMoreMenuItems(Context context, String sessionId,
                                                        SessionTypeEnum sessionTypeEnum) {
        List<PopupMenuItem> moreMenuItems = new ArrayList<PopupMenuItem>();
        moreMenuItems.add(new PopupMenuItem(context, ACTION_HISTORY_QUERY, sessionId, sessionTypeEnum,
                ApplicationUtils.getContext().getString(R.string.message_history_query)));
        moreMenuItems.add(new PopupMenuItem(context, ACTION_SEARCH_MESSAGE, sessionId, sessionTypeEnum,
                ApplicationUtils.getContext().getString(R.string.message_search_title)));
        moreMenuItems.add(new PopupMenuItem(context, ACTION_CLEAR_MESSAGE, sessionId, sessionTypeEnum,
                ApplicationUtils.getContext().getString(R.string.message_clear)));
        if (sessionTypeEnum == SessionTypeEnum.P2P) {
            moreMenuItems.add(new PopupMenuItem(context, ACTION_CLEAR_P2P_MESSAGE, sessionId, sessionTypeEnum,
                    ApplicationUtils.getContext().getString(R.string.message_p2p_clear)));
        }
        return moreMenuItems;
    }
}
