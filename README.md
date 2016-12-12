# RefreshNestedLayout
This project aims to provide a powerful and convenient widget for Android, include Pull-To-Refresh, Auto-Load, First-Screen-Loading. It was inspired by [Chris Banes / Android-PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh), [Google / SwipeRefreshLayout](https://developer.android.com/reference/android/support/v4/widget/SwipeRefreshLayout.html)(mainly for NestedScrolling) and [JoanZapata / base-adapter-helper](https://github.com/JoanZapata/base-adapter-helper)(make its convenient when write adapter).

# Features
* Supports Pull-To-Refresh, Auto-Load and First-Screen-Loading.
* Easy to replace refresh head view.
* Customizable layout, e.g. loading Layout, empty layout, auto-load Layout and so on.
* Currently works with **ListView**, **RecycleView** and **ScrollView**, also behave well in **CoordinatorLayout**.
* Two modes of data adapter: Quick and Wrap, like **QuickAdapter** or **WrapAdapter**.

# Sample Application
DownLoad and run it. (I know it's a bad experience.)

# Usage

### Layout

The first thing to do is to modify your layout file to reference one of the RefreshNestedLayout Views instead of an Android platform View (such as ListView), as so:

``` 

    <com.ycdyng.refreshnestedlayout.RefreshNestedListViewLayout
        android:id="@+id/refresh_layout"
        android:layout_width="match_parent"
        android:layout_height="fill_parent" />
        
```

### Pull-To-Refresh

``` java
...

mRefreshLayout = (RefreshNestedListViewLayout) findViewById(R.id.refresh_layout);
// set pull-to-refresh Listener
mRefreshLayout.setOnRefreshListener(new RefreshNestedLayout.OnRefreshListener() {

    @Override
    public void onRefresh() {
        
        ...
        
        // Call onRefreshComplete when the ListView's dataset has been refreshed.
        mRefreshLayout.onRefreshComplete();
    }
});

...
```

### Auto-Load

``` java
...

mRefreshLayout.setAutoLoadUsable(true);
// set auto-load Listener
mRefreshLayout.setOnAutoLoadListener(new RefreshNestedLayout.OnAutoLoadListener() {

    @Override
    public void onLoading() {
        
        ...
        
        //When data has been loaded, call onAutoLoadingComplete.
        mRefreshLayout.onAutoLoadingComplete(true);
        
        // if no more data, you should call below:
        // mRefreshLayout.setAutoLoadUsable(false);
        // mRefreshLayout.onAutoLoadingComplete(false);
    }
});

...
```
Note: This feature is currently not support ScrollView.

### First-Screen-Loading

``` java
...

// begin loading data
mRefreshLayout.onLoadingDataStart();

mRefreshLayout.postDelayed(new Runnable() {

    @Override
    public void run() {
    
        ...

        //When data has been loaded, call onLoadingDataComplete.
        // if data empty, will show empty layout.
        mRefreshLayout.onLoadingDataComplete();
    }
}, 3000);

...
```

### Adapter
There are two modes of data adapter: Quick-Adapter and Wrap-Adapter.

#### Quick-Adapter

this is a convenient way to write adapter, more detail see [JoanZapata/base-adapter-helper's usage](https://github.com/JoanZapata/base-adapter-helper).

``` java
...

private QuickAdapter<SampleModel> mQuickAdapter;
private List<SampleModel> mDataList = new ArrayList<SampleModel>();

...
    
mQuickAdapter = new QuickAdapter<SampleModel>(this, R.layout.list_item, mDataList) {

    @Override
    protected void convert(int position, BaseAdapterHelper helper, SampleModel item) {
        helper.setText(R.id.textView1, item.getValues());
    }
};

...

mRefreshLayout.setAdapter(mQuickAdapter);
    
...
```
also the same usage as QuickRecyclerAdapter and QuickRecyclerMultiAdapter.

#### Wrap-Adapter

wrapper existing adapter if need.

``` java
...

private SampleAdapter mSampleAdapter;
private WrapAdapter<SampleAdapter> mWrapAdapter;
private List<SampleModel> mDataList = new ArrayList<SampleModel>();

...

mSampleAdapter = new SampleAdapter(content, mDataList);
mWrapAdapter = new WrapAdapter<SampleAdapter>(mSampleAdapter);

...

mRefreshLayout.setAdapter(mQuickAdapter);

...
```
also the same usage as WrapRecyclerAdapter.

## Suggest

Importing RefreshNestedLayout to your project as a module, modified DefaultHeaderLayout or others default layouts.

## License
```
   Copyright 2016 EastWood Yang

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

