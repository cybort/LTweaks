package li.lingfeng.ltweaks.prefs;

import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by smallville on 2017/1/21.
 */

public class SharedPreferences implements android.content.SharedPreferences {

    public interface OnPreferenceChangeListener {
        void onChanged(String key, Object value);
    }

    public static final String ACTION_PREF_CHANGE_PREFIX = PackageNames.L_TWEAKS + ".ACTION_PREF_CHANGE.";
    private Context mContext;
    private android.content.SharedPreferences mOriginal;
    private Set<String> mRegisteredChangeKeys;
    private Map<String, Object> mChangedValues; // Receive changed values from broadcast
    private Map<String, List<OnPreferenceChangeListener>> mChangeListeners;
    private BroadcastReceiver mValueChangeReceiver;

    public SharedPreferences(Context context, android.content.SharedPreferences original) {
        mContext = context;
        mOriginal = original;
    }

    private Context getContext() {
        return mContext != null ? mContext : AndroidAppHelper.currentApplication();
    }

    private String getKeyById(int id) {
        return PrefKeys.getById(id);
    }

    @Override
    public Map<String, ?> getAll() {
        Map all = mOriginal.getAll();
        if (mChangedValues != null) {
            all = new HashMap<>(all);
            for (Map.Entry<String, ?> kv : mChangedValues.entrySet()) {
                all.put(kv.getKey(), kv.getValue());
            }
        }
        return all;
    }

    @Override
    @Nullable
    public String getString(String key, @Nullable String defValue) {
        if (mChangedValues != null && mChangedValues.containsKey(key)) {
            return (String) mChangedValues.get(key);
        }
        return mOriginal.getString(key, defValue);
    }

    @Nullable
    public String getString(@StringRes int key, @Nullable String defValue) {
        return getString(getKeyById(key), defValue);
    }

    @Override
    @Nullable
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        if (mChangedValues != null && mChangedValues.containsKey(key)) {
            return (Set<String>) mChangedValues.get(key);
        }
        return mOriginal.getStringSet(key, defValues);
    }

    @Nullable
    public Set<String> getStringSet(@StringRes int key, @Nullable Set<String> defValues) {
        return getStringSet(getKeyById(key), defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        if (mChangedValues != null && mChangedValues.containsKey(key)) {
            return (int) mChangedValues.get(key);
        }
        return mOriginal.getInt(key, defValue);
    }

    public int getInt(@StringRes int key, int defValue) {
        return getInt(getKeyById(key), defValue);
    }

    public int getIntFromString(String key, int defValue) {
        String s = getString(key, String.valueOf(defValue));
        return Integer.parseInt(s);
    }

    public int getIntFromString(@StringRes int key, int defValue) {
        return getIntFromString(getKeyById(key), defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        if (mChangedValues != null && mChangedValues.containsKey(key)) {
            return (long) mChangedValues.get(key);
        }
        return mOriginal.getLong(key, defValue);
    }

    public long getLong(@StringRes int key, long defValue) {
        return getLong(getKeyById(key), defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        if (mChangedValues != null && mChangedValues.containsKey(key)) {
            return (float) mChangedValues.get(key);
        }
        return mOriginal.getFloat(key, defValue);
    }

    public float getFloat(@StringRes int key, float defValue) {
        return getFloat(getKeyById(key), defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (mChangedValues != null && mChangedValues.containsKey(key)) {
            return (boolean) mChangedValues.get(key);
        }
        return mOriginal.getBoolean(key, defValue);
    }

    public boolean getBoolean(@StringRes int key, boolean defValue) {
        return getBoolean(getKeyById(key), defValue);
    }

    @Override
    public boolean contains(String key) {
        return mOriginal.contains(key);
    }

    public boolean contains(@StringRes int key) {
        return contains(getKeyById(key));
    }

    @Override
    public Editor edit() {
        return new Editor(mOriginal.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mOriginal.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mOriginal.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public void registerPreferenceChangeKey(@StringRes int key) {
        registerPreferenceChangeKey(key, null);
    }

    public void registerPreferenceChangeKey(@StringRes int key, final OnPreferenceChangeListener changeListener) {
        registerPreferenceChangeKey(getKeyById(key), changeListener);
    }

    public void registerPreferenceChangeKey(String key) {
        registerPreferenceChangeKey(key, null);
    }

    public void registerPreferenceChangeKey(String key, final OnPreferenceChangeListener changeListener) {
        if (getContext().getPackageName().equals(PackageNames.L_TWEAKS)) {
            return;
        }

        if (mValueChangeReceiver == null) {
            mValueChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (mChangedValues == null) {
                        mChangedValues = new HashMap<>();
                    }
                    String key = intent.getStringExtra("key");
                    Object value = intent.getExtras().get("value");
                    if (value instanceof String[]) {
                        String[] array = (String[]) value;
                        Set<String> set = new HashSet<>(array.length);
                        Collections.addAll(set, array);
                        value = set;
                    }
                    mChangedValues.put(key, value);

                    if (mChangeListeners != null) {
                        List<OnPreferenceChangeListener> listeners = mChangeListeners.get(key);
                        if (listeners != null) {
                            for (OnPreferenceChangeListener listener : listeners) {
                                listener.onChanged(key, value);
                            }
                        }
                    }
                }
            };
        }

        if (changeListener != null) {
            if (mChangeListeners == null) {
                mChangeListeners = new HashMap<>();
            }
            List<OnPreferenceChangeListener> listeners = mChangeListeners.get(key);
            if (listeners == null) {
                listeners = new ArrayList<>(1);
                mChangeListeners.put(key, listeners);
            }
            listeners.add(changeListener);
        }

        if (mRegisteredChangeKeys == null) {
            mRegisteredChangeKeys = new HashSet<>();
        }
        if (!mRegisteredChangeKeys.contains(key)) {
            mRegisteredChangeKeys.add(key);
            IntentFilter filter = new IntentFilter(ACTION_PREF_CHANGE_PREFIX + key);
            filter.setPriority(999);
            getContext().registerReceiver(mValueChangeReceiver, filter);
        }
    }

    public class Editor implements android.content.SharedPreferences.Editor {

        private android.content.SharedPreferences.Editor mOriginal;

        public Editor(android.content.SharedPreferences.Editor original) {
            mOriginal = original;
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            mOriginal.putString(key, value);
            return this;
        }

        public Editor putString(@StringRes int key, @Nullable String value) {
            putString(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            mOriginal.putStringSet(key, values);
            return this;
        }

        public Editor putStringSet(@StringRes int key, @Nullable Set<String> values) {
            putStringSet(getKeyById(key), values);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            mOriginal.putInt(key, value);
            return this;
        }

        public Editor putInt(@StringRes int key, int value) {
            putInt(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            mOriginal.putLong(key, value);
            return this;
        }

        public Editor putLong(@StringRes int key, long value) {
            putLong(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            mOriginal.putFloat(key, value);
            return this;
        }

        public Editor putFloat(@StringRes int key, float value) {
            putFloat(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            mOriginal.putBoolean(key, value);
            return this;
        }

        public Editor putBoolean(@StringRes int key, boolean value) {
            putBoolean(getKeyById(key), value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            mOriginal.remove(key);
            return this;
        }

        public Editor remove(@StringRes int key) {
            remove(getKeyById(key));
            return this;
        }

        @Override
        public Editor clear() {
            mOriginal.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return mOriginal.commit();
        }

        @Override
        public void apply() {
            mOriginal.apply();
        }
    }
}
