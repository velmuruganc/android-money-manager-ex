package com.money.manager.ex.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.money.manager.ex.R;
import com.money.manager.ex.businessobjects.AccountService;
import com.money.manager.ex.currency.CurrencyService;
import com.money.manager.ex.datalayer.AccountRepository;
import com.money.manager.ex.database.QueryAccountBills;
import com.money.manager.ex.database.WhereStatementGenerator;
import com.money.manager.ex.domainmodel.Account;
import com.money.manager.ex.home.MainActivity;
import com.money.manager.ex.settings.AppSettings;
import com.money.manager.ex.transactions.EditTransactionActivity;

import org.apache.commons.lang3.StringUtils;

import info.javaperformance.money.Money;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link SingleAccountWidgetConfigureActivity SingleAccountWidgetConfigureActivity}
 */
public class SingleAccountWidget
        extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            SingleAccountWidgetConfigureActivity.deleteTitlePref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onAppWidgetOptionsChanged (Context context,
                                      AppWidgetManager appWidgetManager,
                                      int appWidgetId, Bundle newOptions) {
        // Here you can update your widget view
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        Log.d(this.getClass().getSimpleName(), "resized");

        // Obtain appropriate widget and update it.
        appWidgetManager.updateAppWidget(appWidgetId, getRemoteViews(context, minWidth, minHeight));

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        this.updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    private RemoteViews mRemoteViews;

    private RemoteViews getRemoteViews(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        if (mRemoteViews == null) {
            int minWidth = appWidgetManager.getAppWidgetInfo(appWidgetId).minResizeWidth;
            int minHeight = appWidgetManager.getAppWidgetInfo(appWidgetId).minResizeHeight;
            mRemoteViews = getRemoteViews(context, minWidth, minHeight);
        }
        return mRemoteViews;

    }

    /**
     * Determine appropriate view based on width provided.
     *
     * @param minWidth current width
     * @param minHeight current height
     * @return Remote views for the current widget.
     */
    private RemoteViews getRemoteViews(Context context, int minWidth, int minHeight) {
        // First find out rows and columns based on width provided.
        int rows = getCellsForSize(minHeight);
        int columns = getCellsForSize(minWidth);

        if (columns <= 2) {
            // Get 1 column widget remote view and return
            mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_single_account_1x1);
        } else {
            // Get appropriate remote view.
            mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_single_account);
        }
        return mRemoteViews;
    }

    /**
     * Returns number of cells needed for given size of the widget.
     *
     * @param size Widget size in dp.
     * @return Size in number of cells.
     */
    private static int getCellsForSize(int size) {
        int n = 2;
        while (70 * n - 30 < size) {
            ++n;
        }
        return n - 1;
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        // Construct the RemoteViews object
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_single_account);
        RemoteViews views = getRemoteViews(context, appWidgetManager, appWidgetId);

        // todo: allow selecting the account from a list.

        // todo: load the configured account id
        AppSettings settings = new AppSettings(context);
        String defaultAccountId = settings.getGeneralSettings().getDefaultAccountId();
        if (StringUtils.isNotEmpty(defaultAccountId)) {
            displayAccountInfo(context, defaultAccountId, views);
        }

        // handle + click -> open the new transaction screen for this account.
        // todo: pass the account id?
        initializeNewTransactionCommand(context, views);

        // handle logo click -> open the app.
        initializeStartAppCommand(context, views);

        // click account name -> refresh the balance.
        initializeRefreshDataCommand(context, views, appWidgetId);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void displayAccountInfo(Context context, String defaultAccountId, RemoteViews views) {
        int accountId = Integer.parseInt(defaultAccountId);
        Account account = loadAccount(context, accountId);
        if (account == null) return;

//        CharSequence widgetText = SingleAccountWidgetConfigureActivity.loadTitlePref(context, appWidgetId);

//        views.setTextViewText(R.id.appwidget_text, widgetText);

        // display the account name
//        String accountName = getAccountName(context, accountId);
        String accountName = account.getName();
        views.setTextViewText(R.id.accountNameTextView, accountName);

        // get account balance (for this account?)
        String balance = getFormattedAccountBalance(context, account);
        views.setTextViewText(R.id.balanceTextView, balance);
    }

    private void initializeNewTransactionCommand(Context context, RemoteViews views) {
        Intent intent = new Intent(context, EditTransactionActivity.class);
        intent.setAction(Intent.ACTION_INSERT);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        views.setOnClickPendingIntent(R.id.newTransactionPanel, pendingIntent);
        // for now, the button requires a separate setup. try to find a way to propagate click.
        views.setOnClickPendingIntent(R.id.newTransactionButton, pendingIntent);
    }

    private String getFormattedAccountBalance(Context context, Account account) {
        WhereStatementGenerator where = new WhereStatementGenerator();
        where.addStatement(QueryAccountBills.ACCOUNTID, "=", account.getId());
        String selection =  where.getWhere();

        AccountService service = new AccountService(context);
        Money total = service.loadBalance(selection);

        // format the amount
        CurrencyService currencyService = new CurrencyService(context);
        String summary = currencyService.getCurrencyFormatted(
                account.getCurrencyId(), total);

        return summary;
    }

    private Account loadAccount(Context context, int accountId) {
        AccountRepository repository = new AccountRepository(context);
        return repository.load(accountId);
    }

    private void initializeStartAppCommand(Context context, RemoteViews views) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        // Get the layout for the App Widget and attach an on-click listener to the button
//        RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.appwidget_provider_layout);

        views.setOnClickPendingIntent(R.id.appLogoImage, pendingIntent);
    }

    private void initializeRefreshDataCommand(Context context, RemoteViews views, int appWidgetId) {
        // refresh the balance on tap.
        Intent intent = new Intent(context, SingleAccountWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.refreshDataPanel, pendingIntent);
    }

}
