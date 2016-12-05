package dynoapps.exchange_rates.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import dynoapps.exchange_rates.DataSourcesManager;
import dynoapps.exchange_rates.data.RateDataSource;
import dynoapps.exchange_rates.data.RatesHolder;
import dynoapps.exchange_rates.event.DataSourceUpdate;
import dynoapps.exchange_rates.event.IntervalUpdate;
import dynoapps.exchange_rates.event.RatesEvent;
import dynoapps.exchange_rates.model.BigparaRate;
import dynoapps.exchange_rates.model.DolarTlKurRate;
import dynoapps.exchange_rates.model.EnparaRate;
import dynoapps.exchange_rates.model.YapıKrediRate;
import dynoapps.exchange_rates.model.YorumlarRate;
import dynoapps.exchange_rates.provider.BasePoolingDataProvider;
import dynoapps.exchange_rates.provider.BigparaRateProvider;
import dynoapps.exchange_rates.provider.DolarTlKurRateProvider;
import dynoapps.exchange_rates.provider.EnparaRateProvider;
import dynoapps.exchange_rates.provider.IPollingSource;
import dynoapps.exchange_rates.provider.ProviderSourceCallbackAdapter;
import dynoapps.exchange_rates.provider.YapıKrediRateProvider;
import dynoapps.exchange_rates.provider.YorumlarRateProvider;

/**
 * Created by erdemmac on 05/12/2016.
 */

public class RatePollingService extends Service {
    ArrayList<BasePoolingDataProvider> providers = new ArrayList<>();

    private final IBinder mBinder = new SimpleBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        if (providers.size() > 0) return;
        providers.add(new YorumlarRateProvider(new ProviderSourceCallbackAdapter<List<YorumlarRate>>() {
            @Override
            public void onResult(List<YorumlarRate> rates) {
                RatesHolder.getInstance().addRate(rates, YorumlarRate.class);
                EventBus.getDefault().post(new RatesEvent<>(rates));
            }
        }));
        providers.add(new EnparaRateProvider(new ProviderSourceCallbackAdapter<List<EnparaRate>>() {
            @Override
            public void onResult(List<EnparaRate> rates) {
                RatesHolder.getInstance().addRate(rates, EnparaRate.class);
                EventBus.getDefault().post(new RatesEvent<>(rates));
            }
        }));

        providers.add(
                new BigparaRateProvider(new ProviderSourceCallbackAdapter<List<BigparaRate>>() {
                    @Override
                    public void onResult(List<BigparaRate> rates) {
                        RatesHolder.getInstance().addRate(rates, BigparaRate.class);
                        EventBus.getDefault().post(new RatesEvent<>(rates));
                    }
                }));

        providers.add(new DolarTlKurRateProvider(new ProviderSourceCallbackAdapter<List<DolarTlKurRate>>() {
            @Override
            public void onResult(List<DolarTlKurRate> rates) {
                RatesHolder.getInstance().addRate(rates, DolarTlKurRate.class);
                EventBus.getDefault().post(new RatesEvent<>(rates));
            }
        }));


        providers.add(new YapıKrediRateProvider(new ProviderSourceCallbackAdapter<List<YapıKrediRate>>() {
            @Override
            public void onResult(List<YapıKrediRate> rates) {
                RatesHolder.getInstance().addRate(rates, YapıKrediRate.class);
                EventBus.getDefault().post(new RatesEvent<>(rates));
            }
        }));
        DataSourcesManager.init();
        DataSourcesManager.updateProviders(providers);
        refreshSources();
    }

    @Subscribe
    public void onEvent(DataSourceUpdate event) {
        refreshSources();
    }

    @Subscribe
    public void onEvent(IntervalUpdate event) {
        for (BasePoolingDataProvider provider : providers) {
            provider.refreshForIntervals();
        }
    }

    private void refreshSources() {
        ArrayList<RateDataSource> rateDataSources = DataSourcesManager.getRateDataSources();
        for (RateDataSource rateDataSource : rateDataSources) {
            IPollingSource iPollingSource = rateDataSource.getPollingSource();
            if (rateDataSource.isEnabled()) {
                iPollingSource.start();
            } else {
                iPollingSource.stop();
            }
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (providers != null) {
            for (IPollingSource iPollingSource : providers) {
                iPollingSource.stop();
            }
        }
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    public class SimpleBinder extends Binder {
        public RatePollingService getService() {
            return RatePollingService.this;
        }
    }
}