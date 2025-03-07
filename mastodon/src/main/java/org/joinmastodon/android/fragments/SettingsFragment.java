package org.joinmastodon.android.fragments;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.GlobalUserPreferences.ColorPreference;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.PushSubscriptionManager;
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.PushNotification;
import org.joinmastodon.android.model.PushSubscription;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.ArrayList;
import java.util.function.Consumer;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.UsableRecyclerView;

public class SettingsFragment extends MastodonToolbarFragment{
	private UsableRecyclerView list;
	private ArrayList<Item> items=new ArrayList<>();
	private ThemeItem themeItem;
	private NotificationPolicyItem notificationPolicyItem;
	private String accountID;
	private boolean needUpdateNotificationSettings;
	private boolean needAppRestart;
	private PushSubscription pushSubscription;

	private ImageView themeTransitionWindowView;
	private TextItem checkForUpdateItem;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);
		setTitle(R.string.settings);
		accountID=getArguments().getString("account");
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);

		if(GithubSelfUpdater.needSelfUpdating()){
			GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
			GithubSelfUpdater.UpdateState state=updater.getState();
			if(state!=GithubSelfUpdater.UpdateState.NO_UPDATE && state!=GithubSelfUpdater.UpdateState.CHECKING){
				items.add(new UpdateItem());
			}
		}

		items.add(new HeaderItem(R.string.settings_theme));
		items.add(themeItem=new ThemeItem());
		items.add(new SwitchItem(R.string.theme_true_black, R.drawable.ic_fluent_dark_theme_24_regular, GlobalUserPreferences.trueBlackTheme, this::onTrueBlackThemeChanged));
		items.add(new SwitchItem(R.string.sk_disable_marquee, R.drawable.ic_fluent_text_more_24_regular, GlobalUserPreferences.disableMarquee, i->{
			GlobalUserPreferences.disableMarquee=i.checked;
			GlobalUserPreferences.save();
		}));
		items.add(new ColorPalettePicker());

		items.add(new HeaderItem(R.string.settings_behavior));
		items.add(new SwitchItem(R.string.settings_gif, R.drawable.ic_fluent_gif_24_regular, GlobalUserPreferences.playGifs, i->{
			GlobalUserPreferences.playGifs=i.checked;
			GlobalUserPreferences.save();
		}));
		items.add(new SwitchItem(R.string.settings_custom_tabs, R.drawable.ic_fluent_link_24_regular, GlobalUserPreferences.useCustomTabs, i->{
			GlobalUserPreferences.useCustomTabs=i.checked;
			GlobalUserPreferences.save();
		}));
		items.add(new SwitchItem(R.string.sk_settings_show_interaction_counts, R.drawable.ic_fluent_number_row_24_regular, GlobalUserPreferences.showInteractionCounts, i->{
			GlobalUserPreferences.showInteractionCounts=i.checked;
			GlobalUserPreferences.save();
		}));
		items.add(new SwitchItem(R.string.sk_settings_always_reveal_content_warnings, R.drawable.ic_fluent_chat_warning_24_regular, GlobalUserPreferences.alwaysExpandContentWarnings, i->{
			GlobalUserPreferences.alwaysExpandContentWarnings=i.checked;
			GlobalUserPreferences.save();
		}));
		items.add(new SwitchItem(R.string.sk_tabs_disable_swipe, R.drawable.ic_fluent_swipe_right_24_regular, GlobalUserPreferences.disableSwipe, i->{
			GlobalUserPreferences.disableSwipe=i.checked;
			GlobalUserPreferences.save();
			needAppRestart=true;
		}));

		items.add(new HeaderItem(R.string.home_timeline));
		items.add(new SwitchItem(R.string.sk_settings_show_replies, R.drawable.ic_fluent_chat_multiple_24_regular, GlobalUserPreferences.showReplies, i->{
			GlobalUserPreferences.showReplies=i.checked;
			GlobalUserPreferences.save();
		}));
		items.add(new SwitchItem(R.string.sk_settings_show_boosts, R.drawable.ic_fluent_arrow_repeat_all_24_regular, GlobalUserPreferences.showBoosts, i->{
			GlobalUserPreferences.showBoosts=i.checked;
			GlobalUserPreferences.save();
		}));
		items.add(new SwitchItem(R.string.sk_settings_load_new_posts, R.drawable.ic_fluent_arrow_up_24_regular, GlobalUserPreferences.loadNewPosts, i->{
			GlobalUserPreferences.loadNewPosts=i.checked;
			GlobalUserPreferences.save();
		}));
		items.add(new SwitchItem(R.string.sk_settings_show_federated_timeline, R.drawable.ic_fluent_earth_24_regular, GlobalUserPreferences.showFederatedTimeline, i->{
			GlobalUserPreferences.showFederatedTimeline=i.checked;
			GlobalUserPreferences.save();
			needAppRestart=true;
		}));

		items.add(new HeaderItem(R.string.settings_notifications));
		items.add(notificationPolicyItem=new NotificationPolicyItem());
		PushSubscription pushSubscription=getPushSubscription();
		items.add(new SwitchItem(R.string.notify_favorites, R.drawable.ic_fluent_star_24_regular, pushSubscription.alerts.favourite, i->onNotificationsChanged(PushNotification.Type.FAVORITE, i.checked)));
		items.add(new SwitchItem(R.string.notify_follow, R.drawable.ic_fluent_person_add_24_regular, pushSubscription.alerts.follow, i->onNotificationsChanged(PushNotification.Type.FOLLOW, i.checked)));
		items.add(new SwitchItem(R.string.notify_reblog, R.drawable.ic_fluent_arrow_repeat_all_24_regular, pushSubscription.alerts.reblog, i->onNotificationsChanged(PushNotification.Type.REBLOG, i.checked)));
		items.add(new SwitchItem(R.string.notify_mention, R.drawable.ic_at_symbol, pushSubscription.alerts.mention, i->onNotificationsChanged(PushNotification.Type.MENTION, i.checked)));
		items.add(new SwitchItem(R.string.sk_notify_posts, R.drawable.ic_fluent_alert_24_regular, pushSubscription.alerts.status, i->onNotificationsChanged(PushNotification.Type.STATUS, i.checked)));

		items.add(new HeaderItem(R.string.settings_account));
		items.add(new TextItem(R.string.sk_settings_profile, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/settings/profile"), R.drawable.ic_fluent_open_24_regular));
		items.add(new TextItem(R.string.sk_settings_posting, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/settings/preferences/other"), R.drawable.ic_fluent_open_24_regular));
		items.add(new TextItem(R.string.sk_settings_filters, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/filters"), R.drawable.ic_fluent_open_24_regular));
		items.add(new TextItem(R.string.sk_settings_auth, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/auth/edit"), R.drawable.ic_fluent_open_24_regular));

		Instance instance = AccountSessionManager.getInstance().getInstanceInfo(session.domain);
		items.add(new HeaderItem(instance != null ? instance.title : session.domain));
		items.add(new TextItem(R.string.sk_settings_rules, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/about"), R.drawable.ic_fluent_open_24_regular));
		items.add(new TextItem(R.string.settings_tos, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/terms"), R.drawable.ic_fluent_open_24_regular));
		items.add(new TextItem(R.string.settings_privacy_policy, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/terms"), R.drawable.ic_fluent_open_24_regular));
		items.add(new TextItem(R.string.log_out, this::confirmLogOut, R.drawable.ic_fluent_sign_out_24_regular));

		items.add(new HeaderItem(R.string.sk_settings_about));
		items.add(new TextItem(R.string.sk_settings_contribute, ()->UiUtils.launchWebBrowser(getActivity(), "https://github.com/sk22/megalodon"), R.drawable.ic_fluent_open_24_regular));
		items.add(new TextItem(R.string.sk_settings_donate, ()->UiUtils.launchWebBrowser(getActivity(), "https://ko-fi.com/xsk22"), R.drawable.ic_fluent_open_24_regular));
		if (GithubSelfUpdater.needSelfUpdating()) {
			checkForUpdateItem = new TextItem(R.string.sk_check_for_update, GithubSelfUpdater.getInstance()::checkForUpdates);
			items.add(checkForUpdateItem);
		}
		items.add(new TextItem(R.string.settings_clear_cache, this::clearImageCache));
		items.add(new TextItem(R.string.sk_clear_recent_languages, ()->UiUtils.showConfirmationAlert(getActivity(), R.string.sk_clear_recent_languages, R.string.sk_confirm_clear_recent_languages, R.string.clear, ()->{
			GlobalUserPreferences.recentLanguages.remove(accountID);
			GlobalUserPreferences.save();
		})));

		items.add(new FooterItem(getString(R.string.sk_settings_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)));
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		if(themeTransitionWindowView!=null){
			// Activity has finished recreating. Remove the overlay.
			MastodonApp.context.getSystemService(WindowManager.class).removeView(themeTransitionWindowView);
			themeTransitionWindowView=null;
		}
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		list=new UsableRecyclerView(getActivity());
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		list.setAdapter(new SettingsAdapter());
		list.setBackgroundColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorBackground));
		list.setPadding(0, V.dp(16), 0, V.dp(12));
		list.setClipToPadding(false);
		list.addItemDecoration(new RecyclerView.ItemDecoration(){
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
				// Add 32dp gaps between sections
				RecyclerView.ViewHolder holder=parent.getChildViewHolder(view);
				if((holder instanceof HeaderViewHolder || holder instanceof FooterViewHolder) && holder.getAbsoluteAdapterPosition()>1)
					outRect.top=V.dp(32);
			}
		});
		return list;
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
			list.setPadding(0, V.dp(16), 0, V.dp(12)+insets.getSystemWindowInsetBottom());
			insets=insets.inset(0, 0, 0, insets.getSystemWindowInsetBottom());
		}
		super.onApplyWindowInsets(insets);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(needUpdateNotificationSettings && PushSubscriptionManager.arePushNotificationsAvailable()){
			AccountSessionManager.getInstance().getAccount(accountID).getPushSubscriptionManager().updatePushSettings(pushSubscription);
		}
		if(needAppRestart){
			Intent intent = Intent.makeRestartActivityTask(MastodonApp.context.getPackageManager().getLaunchIntentForPackage(MastodonApp.context.getPackageName()).getComponent());
			MastodonApp.context.startActivity(intent);
			Runtime.getRuntime().exit(0);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		if(GithubSelfUpdater.needSelfUpdating())
			E.register(this);
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
		if(GithubSelfUpdater.needSelfUpdating())
			E.unregister(this);
	}

	private void onThemePreferenceClick(GlobalUserPreferences.ThemePreference theme){
		GlobalUserPreferences.theme=theme;
		GlobalUserPreferences.save();
		restartActivityToApplyNewTheme();
	}

	private boolean onColorPreferenceClick(MenuItem item){
		ColorPreference pref = null;
		int id = item.getItemId();

		if (id == R.id.m3_color) pref = ColorPreference.MATERIAL3;
		else if (id == R.id.pink_color) pref = ColorPreference.PINK;
		else if (id == R.id.purple_color) pref = ColorPreference.PURPLE;
		else if (id == R.id.green_color) pref = ColorPreference.GREEN;
		else if (id == R.id.blue_color) pref = ColorPreference.BLUE;
		else if (id == R.id.brown_color) pref = ColorPreference.BROWN;
		else if (id == R.id.red_color) pref = ColorPreference.RED;
		else if (id == R.id.yellow_color) pref = ColorPreference.YELLOW;
		else if (id == R.id.sussyred_color) pref = ColorPreference.SUSSYRED;

		if (pref == null) return false;

		GlobalUserPreferences.color=pref;
		GlobalUserPreferences.save();
		restartActivityToApplyNewTheme();
		return true;
	}

	private void onTrueBlackThemeChanged(SwitchItem item){
		GlobalUserPreferences.trueBlackTheme=item.checked;
		GlobalUserPreferences.save();

		RecyclerView.ViewHolder themeHolder=list.findViewHolderForAdapterPosition(items.indexOf(themeItem));
		if(themeHolder!=null){
			((ThemeViewHolder)themeHolder).bindSubitems();
		}else{
			list.getAdapter().notifyItemChanged(items.indexOf(themeItem));
		}

		if(UiUtils.isDarkTheme()){
			restartActivityToApplyNewTheme();
		}
	}

	private void restartActivityToApplyNewTheme(){
		// Calling activity.recreate() causes a black screen for like half a second.
		// So, let's take a screenshot and overlay it on top to create the illusion of a smoother transition.
		// As a bonus, we can fade it out to make it even smoother.
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
			View activityDecorView=getActivity().getWindow().getDecorView();
			Bitmap bitmap=Bitmap.createBitmap(activityDecorView.getWidth(), activityDecorView.getHeight(), Bitmap.Config.ARGB_8888);
			activityDecorView.draw(new Canvas(bitmap));
			themeTransitionWindowView=new ImageView(MastodonApp.context);
			themeTransitionWindowView.setImageBitmap(bitmap);
			WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION);
			lp.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
					WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
			lp.systemUiVisibility=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			lp.systemUiVisibility|=(activityDecorView.getWindowSystemUiVisibility() & (View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
			lp.width=lp.height=WindowManager.LayoutParams.MATCH_PARENT;
			lp.token=getActivity().getWindow().getAttributes().token;
			lp.windowAnimations=R.style.window_fade_out;
			MastodonApp.context.getSystemService(WindowManager.class).addView(themeTransitionWindowView, lp);
		}
		getActivity().recreate();
	}

	private PushSubscription getPushSubscription(){
		if(pushSubscription!=null)
			return pushSubscription;
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		if(session.pushSubscription==null){
			pushSubscription=new PushSubscription();
			pushSubscription.alerts=PushSubscription.Alerts.ofAll();
		}else{
			pushSubscription=session.pushSubscription.clone();
		}
		return pushSubscription;
	}

	private void onNotificationsChanged(PushNotification.Type type, boolean enabled){
		PushSubscription subscription=getPushSubscription();
		switch(type){
			case FAVORITE -> subscription.alerts.favourite=enabled;
			case FOLLOW -> subscription.alerts.follow=enabled;
			case REBLOG -> subscription.alerts.reblog=enabled;
			case MENTION -> subscription.alerts.mention=subscription.alerts.poll=enabled;
			case STATUS -> subscription.alerts.status=enabled;
		}
		needUpdateNotificationSettings=true;
	}

	private void onNotificationsPolicyChanged(PushSubscription.Policy policy){
		PushSubscription subscription=getPushSubscription();
		PushSubscription.Policy prevPolicy=subscription.policy;
		if(prevPolicy==policy)
			return;
		subscription.policy=policy;
		int index=items.indexOf(notificationPolicyItem);
		RecyclerView.ViewHolder policyHolder=list.findViewHolderForAdapterPosition(index);
		if(policyHolder!=null){
			((NotificationPolicyViewHolder)policyHolder).rebind();
		}else{
			list.getAdapter().notifyItemChanged(index);
		}
		if((prevPolicy==PushSubscription.Policy.NONE)!=(policy==PushSubscription.Policy.NONE)){
			index++;
			while(items.get(index) instanceof SwitchItem si){
				si.enabled=si.checked=policy!=PushSubscription.Policy.NONE;
				RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(index);
				if(holder!=null)
					((BindableViewHolder<?>)holder).rebind();
				else
					list.getAdapter().notifyItemChanged(index);
				index++;
			}
		}
		needUpdateNotificationSettings=true;
	}

	private void confirmLogOut(){
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.log_out)
				.setMessage(R.string.confirm_log_out)
				.setPositiveButton(R.string.log_out, (dialog, which) -> logOut())
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void logOut(){
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		new RevokeOauthToken(session.app.clientId, session.app.clientSecret, session.token.accessToken)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						onLoggedOut();
					}

					@Override
					public void onError(ErrorResponse error){
						onLoggedOut();
					}
				})
				.wrapProgress(getActivity(), R.string.loading, false)
				.exec(accountID);
	}

	private void onLoggedOut(){
		AccountSessionManager.getInstance().removeAccount(accountID);
		getActivity().finish();
		Intent intent=new Intent(getActivity(), MainActivity.class);
		startActivity(intent);
	}

	private void clearImageCache(){
		MastodonAPIController.runInBackground(()->{
			Activity activity=getActivity();
			ImageCache.getInstance(getActivity()).clear();
			Toast.makeText(activity, R.string.media_cache_cleared, Toast.LENGTH_SHORT).show();
		});
	}

	@Subscribe
	public void onSelfUpdateStateChanged(SelfUpdateStateChangedEvent ev){
		checkForUpdateItem.loading = ev.state == GithubSelfUpdater.UpdateState.CHECKING;
		if (list.findViewHolderForAdapterPosition(items.indexOf(checkForUpdateItem)) instanceof TextViewHolder tvh) tvh.rebind();
		
		UpdateItem updateItem = null;
		if(items.get(0) instanceof UpdateItem item0) {
			updateItem = item0;
		} else if (ev.state != GithubSelfUpdater.UpdateState.CHECKING
				&& ev.state != GithubSelfUpdater.UpdateState.NO_UPDATE) {
			updateItem = new UpdateItem();
			items.add(0, updateItem);
			list.setAdapter(new SettingsAdapter());
		}

		if(updateItem != null && list.findViewHolderForAdapterPosition(0) instanceof UpdateViewHolder uvh){
			uvh.bind(updateItem);
		}

		if (ev.state == GithubSelfUpdater.UpdateState.NO_UPDATE) {
			Toast.makeText(getActivity(), R.string.sk_no_update_available, Toast.LENGTH_SHORT).show();
		}
	}

	private static abstract class Item{
		public abstract int getViewType();
	}

	private class HeaderItem extends Item{
		private String text;

		public HeaderItem(@StringRes int text){
			this.text=getString(text);
		}

		public HeaderItem(String text) {
			this.text=text;
		}

		@Override
		public int getViewType(){
			return 0;
		}
	}

	private class SwitchItem extends Item{
		private String text;
		private int icon;
		private boolean checked;
		private Consumer<SwitchItem> onChanged;
		private boolean enabled=true;

		public SwitchItem(@StringRes int text, @DrawableRes int icon, boolean checked, Consumer<SwitchItem> onChanged){
			this.text=getString(text);
			this.icon=icon;
			this.checked=checked;
			this.onChanged=onChanged;
		}

		public SwitchItem(@StringRes int text, int icon, boolean checked, Consumer<SwitchItem> onChanged, boolean enabled){
			this.text=getString(text);
			this.icon=icon;
			this.checked=checked;
			this.onChanged=onChanged;
			this.enabled=enabled;
		}

		@Override
		public int getViewType(){
			return 1;
		}
	}

	public class ColorPalettePicker extends Item{
		@Override
		public int getViewType(){
			return 8;
		}
	}

	private static class ThemeItem extends Item{

		@Override
		public int getViewType(){
			return 2;
		}
	}

	private static class NotificationPolicyItem extends Item{

		@Override
		public int getViewType(){
			return 3;
		}
	}

	private class TextItem extends Item{
		private String text;
		private Runnable onClick;
		private boolean loading;
		private int icon;

		public TextItem(@StringRes int text, Runnable onClick) {
			this(text, onClick, false, 0);
		}

		public TextItem(@StringRes int text, Runnable onClick, boolean loading) {
			this(text, onClick, loading, 0);
		}

		public TextItem(@StringRes int text, Runnable onClick, @DrawableRes int icon) {
			this(text, onClick, false, icon);
		}

		public TextItem(@StringRes int text, Runnable onClick, boolean loading, @DrawableRes int icon){
			this.text=getString(text);
			this.onClick=onClick;
			this.loading=loading;
			this.icon=icon;
		}

		@Override
		public int getViewType(){
			return 4;
		}
	}

	private class RedHeaderItem extends HeaderItem{

		public RedHeaderItem(int text){
			super(text);
		}

		@Override
		public int getViewType(){
			return 5;
		}
	}

	private class FooterItem extends Item{
		private String text;

		public FooterItem(String text){
			this.text=text;
		}

		@Override
		public int getViewType(){
			return 6;
		}
	}

	private class UpdateItem extends Item{

		@Override
		public int getViewType(){
			return 7;
		}
	}

	private class SettingsAdapter extends RecyclerView.Adapter<BindableViewHolder<Item>>{
		@NonNull
		@Override
		public BindableViewHolder<Item> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			//noinspection unchecked
			return (BindableViewHolder<Item>) switch(viewType){
				case 0 -> new HeaderViewHolder(false);
				case 1 -> new SwitchViewHolder();
				case 2 -> new ThemeViewHolder();
				case 3 -> new NotificationPolicyViewHolder();
				case 4 -> new TextViewHolder();
				case 5 -> new HeaderViewHolder(true);
				case 6 -> new FooterViewHolder();
				case 7 -> new UpdateViewHolder();
				case 8 -> new ColorPalettePickerViewHolder();
				default -> throw new IllegalStateException("Unexpected value: "+viewType);
			};
		}

		@Override
		public void onBindViewHolder(@NonNull BindableViewHolder<Item> holder, int position){
			holder.bind(items.get(position));
		}

		@Override
		public int getItemCount(){
			return items.size();
		}

		@Override
		public int getItemViewType(int position){
			return items.get(position).getViewType();
		}
	}

	private class HeaderViewHolder extends BindableViewHolder<HeaderItem>{
		private final TextView text;
		public HeaderViewHolder(boolean red){
			super(getActivity(), R.layout.item_settings_header, list);
			text=(TextView) itemView;
			if(red)
				text.setTextColor(getResources().getColor(UiUtils.isDarkTheme() ? R.color.error_400 : R.color.error_700));
		}

		@Override
		public void onBind(HeaderItem item){
			text.setText(item.text);
		}
	}

	private class SwitchViewHolder extends BindableViewHolder<SwitchItem> implements UsableRecyclerView.DisableableClickable{
		private final TextView text;
		private final ImageView icon;
		private final Switch checkbox;

		public SwitchViewHolder(){
			super(getActivity(), R.layout.item_settings_switch, list);
			text=findViewById(R.id.text);
			icon=findViewById(R.id.icon);
			checkbox=findViewById(R.id.checkbox);
		}

		@Override
		public void onBind(SwitchItem item){
			text.setText(item.text);
			icon.setImageResource(item.icon);
			checkbox.setChecked(item.checked && item.enabled);
			checkbox.setEnabled(item.enabled);
		}

		@Override
		public void onClick(){
			item.checked=!item.checked;
			checkbox.setChecked(item.checked);
			item.onChanged.accept(item);
		}

		@Override
		public boolean isEnabled(){
			return item.enabled;
		}
	}

	private class ThemeViewHolder extends BindableViewHolder<ThemeItem>{
		private SubitemHolder autoHolder, lightHolder, darkHolder;

		public ThemeViewHolder(){
			super(getActivity(), R.layout.item_settings_theme, list);
			autoHolder=new SubitemHolder(findViewById(R.id.theme_auto));
			lightHolder=new SubitemHolder(findViewById(R.id.theme_light));
			darkHolder=new SubitemHolder(findViewById(R.id.theme_dark));
		}

		@Override
		public void onBind(ThemeItem item){
			bindSubitems();
		}

		public void bindSubitems(){
			autoHolder.bind(R.string.theme_auto, GlobalUserPreferences.trueBlackTheme ? R.drawable.theme_auto_trueblack : R.drawable.theme_auto, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.AUTO);
			lightHolder.bind(R.string.theme_light, R.drawable.theme_light, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.LIGHT);
			darkHolder.bind(R.string.theme_dark, GlobalUserPreferences.trueBlackTheme ? R.drawable.theme_dark_trueblack : R.drawable.theme_dark, GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.DARK);
		}

		private void onSubitemClick(View v){
			GlobalUserPreferences.ThemePreference pref;
			if(v.getId()==R.id.theme_auto)
				pref=GlobalUserPreferences.ThemePreference.AUTO;
			else if(v.getId()==R.id.theme_light)
				pref=GlobalUserPreferences.ThemePreference.LIGHT;
			else if(v.getId()==R.id.theme_dark)
				pref=GlobalUserPreferences.ThemePreference.DARK;
			else
				return;
			onThemePreferenceClick(pref);
		}

		private class SubitemHolder{
			public TextView text;
			public ImageView icon;
			public RadioButton checkbox;

			public SubitemHolder(View view){
				text=view.findViewById(R.id.text);
				icon=view.findViewById(R.id.icon);
				checkbox=view.findViewById(R.id.checkbox);
				view.setOnClickListener(ThemeViewHolder.this::onSubitemClick);

				icon.setClipToOutline(true);
				icon.setOutlineProvider(OutlineProviders.roundedRect(4));
			}

			public void bind(int text, int icon, boolean checked){
				this.text.setText(text);
				this.icon.setImageResource(icon);
				checkbox.setChecked(checked);
			}

			public void setChecked(boolean checked){
				checkbox.setChecked(checked);
			}
		}
	}
	private class ColorPalettePickerViewHolder extends BindableViewHolder<ColorPalettePicker>{
		private final Button button;
		private final PopupMenu popupMenu;
		private final ImageView icon;

		@SuppressLint("ClickableViewAccessibility")
		public ColorPalettePickerViewHolder(){
			super(getActivity(), R.layout.item_settings_color_picker, list);
			icon=findViewById(R.id.icon);
			button=findViewById(R.id.color_palette_button);
			popupMenu=new PopupMenu(getActivity(), button, Gravity.CENTER_HORIZONTAL);
			popupMenu.inflate(R.menu.color_palettes);
			popupMenu.getMenu().findItem(R.id.m3_color).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
			popupMenu.setOnMenuItemClickListener(SettingsFragment.this::onColorPreferenceClick);
			button.setOnTouchListener(popupMenu.getDragToOpenListener());
			button.setOnClickListener(v->popupMenu.show());
		}

		@Override
		public void onBind(ColorPalettePicker item){
			icon.setImageResource(R.drawable.ic_fluent_color_24_regular);
			button.setText(switch(GlobalUserPreferences.color){
				case MATERIAL3 -> R.string.sk_color_palette_material3;
				case PINK -> R.string.sk_color_palette_pink;
				case PURPLE -> R.string.sk_color_palette_purple;
				case GREEN -> R.string.sk_color_palette_green;
				case BLUE -> R.string.sk_color_palette_blue;
				case BROWN -> R.string.sk_color_palette_brown;
				case RED -> R.string.sk_color_palette_red;
				case YELLOW -> R.string.sk_color_palette_yellow;
				case SUSSYRED -> R.string.sk_color_pallete_sussyred;
				default -> throw new IllegalStateException("Unexpected value: "+GlobalUserPreferences.color);
			});
		}
	}


	private class NotificationPolicyViewHolder extends BindableViewHolder<NotificationPolicyItem>{
		private final Button button;
		private final PopupMenu popupMenu;

		@SuppressLint("ClickableViewAccessibility")
		public NotificationPolicyViewHolder(){
			super(getActivity(), R.layout.item_settings_notification_policy, list);
			button=findViewById(R.id.button);
			popupMenu=new PopupMenu(getActivity(), button, Gravity.CENTER_HORIZONTAL);
			popupMenu.inflate(R.menu.notification_policy);
			popupMenu.setOnMenuItemClickListener(item->{
				PushSubscription.Policy policy;
				int id=item.getItemId();
				if(id==R.id.notify_anyone)
					policy=PushSubscription.Policy.ALL;
				else if(id==R.id.notify_followed)
					policy=PushSubscription.Policy.FOLLOWED;
				else if(id==R.id.notify_follower)
					policy=PushSubscription.Policy.FOLLOWER;
				else if(id==R.id.notify_none)
					policy=PushSubscription.Policy.NONE;
				else
					return false;
				onNotificationsPolicyChanged(policy);
				return true;
			});
			UiUtils.enablePopupMenuIcons(getActivity(), popupMenu);
			button.setOnTouchListener(popupMenu.getDragToOpenListener());
			button.setOnClickListener(v->popupMenu.show());
		}

		@Override
		public void onBind(NotificationPolicyItem item){
			button.setText(switch(getPushSubscription().policy){
				case ALL -> R.string.notify_anyone;
				case FOLLOWED -> R.string.notify_followed;
				case FOLLOWER -> R.string.notify_follower;
				case NONE -> R.string.notify_none;
			});
		}
	}

	private class TextViewHolder extends BindableViewHolder<TextItem> implements UsableRecyclerView.Clickable{
		private final TextView text;
		private final ProgressBar progress;
		private final ImageView icon;

		public TextViewHolder(){
			super(getActivity(), R.layout.item_settings_text, list);
			text = itemView.findViewById(R.id.text);
			progress = itemView.findViewById(R.id.progress);
			icon = itemView.findViewById(R.id.icon);
		}

		@Override
		public void onBind(TextItem item){
			text.setText(item.text);
			progress.animate().alpha(item.loading ? 1 : 0);
			if (item.icon != 0) icon.setImageDrawable(getActivity().getTheme().getDrawable(item.icon));
		}

		@Override
		public void onClick(){
			item.onClick.run();
		}
	}

	private class FooterViewHolder extends BindableViewHolder<FooterItem>{
		private final TextView text;
		public FooterViewHolder(){
			super(getActivity(), R.layout.item_settings_footer, list);
			text=(TextView) itemView;
		}

		@Override
		public void onBind(FooterItem item){
			text.setText(item.text);
		}
	}

	private class UpdateViewHolder extends BindableViewHolder<UpdateItem>{

		private final TextView text;
		private final Button button;
		private final ImageButton cancelBtn;
		private final ProgressBar progress;

		private ObjectAnimator rotationAnimator;
		private Runnable progressUpdater=this::updateProgress;

		public UpdateViewHolder(){
			super(getActivity(), R.layout.item_settings_update, list);
			text=findViewById(R.id.text);
			button=findViewById(R.id.button);
			cancelBtn=findViewById(R.id.cancel_btn);
			progress=findViewById(R.id.progress);
			button.setOnClickListener(v->{
				GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
				switch(updater.getState()){
					case UPDATE_AVAILABLE -> updater.downloadUpdate();
					case DOWNLOADED -> updater.installUpdate(getActivity());
				}
			});
			cancelBtn.setOnClickListener(v->GithubSelfUpdater.getInstance().cancelDownload());
			rotationAnimator=ObjectAnimator.ofFloat(progress, View.ROTATION, 0f, 360f);
			rotationAnimator.setInterpolator(new LinearInterpolator());
			rotationAnimator.setDuration(1500);
			rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
		}

		@Override
		public void onBind(UpdateItem item){
			GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
			GithubSelfUpdater.UpdateState state=updater.getState();
			if (state == GithubSelfUpdater.UpdateState.CHECKING) return;
			GithubSelfUpdater.UpdateInfo info=updater.getUpdateInfo();
			if(state!=GithubSelfUpdater.UpdateState.DOWNLOADED){
				text.setText(getString(R.string.sk_update_available, info.version));
				button.setText(getString(R.string.download_update, UiUtils.formatFileSize(getActivity(), info.size, false)));
			}else{
				text.setText(getString(R.string.sk_update_ready, info.version));
				button.setText(R.string.install_update);
			}
			if(state==GithubSelfUpdater.UpdateState.DOWNLOADING){
				rotationAnimator.start();
				button.setVisibility(View.INVISIBLE);
				cancelBtn.setVisibility(View.VISIBLE);
				progress.setVisibility(View.VISIBLE);
				updateProgress();
			}else{
				rotationAnimator.cancel();
				button.setVisibility(View.VISIBLE);
				cancelBtn.setVisibility(View.GONE);
				progress.setVisibility(View.GONE);
				progress.removeCallbacks(progressUpdater);
			}
		}

		private void updateProgress(){
			GithubSelfUpdater updater=GithubSelfUpdater.getInstance();
			if(updater.getState()!=GithubSelfUpdater.UpdateState.DOWNLOADING)
				return;
			int value=Math.round(progress.getMax()*updater.getDownloadProgress());
			if(Build.VERSION.SDK_INT>=24)
				progress.setProgress(value, true);
			else
				progress.setProgress(value);
			progress.postDelayed(progressUpdater, 1000);
		}
	}
}
