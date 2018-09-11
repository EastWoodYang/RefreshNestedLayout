# RefreshNestedLayout
The RefreshNestedLayout should be used whenever the user can refresh the contents of a view via a vertical swipe gesture. It was inspired by [Chris Banes / Android-PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh), [Google / SwipeRefreshLayout](https://developer.android.com/reference/android/support/v4/widget/SwipeRefreshLayout.html)(mainly for NestedScrolling).

## Features
* **Easy to custom your refresh head view**.
* Work well with ListView, RecycleView and ScrollView ... 
* Compatible with **CoordinatorLayout**

<img src='https://github.com/EastWoodYang/RefreshNestedLayout/blob/master/picture/1.png' height='500'/>


## Get it
RefreshNestedLayout is now available on JCentral.

    implementation 'com.eastwood.common.view:refresh-nested-layout:1.0.0'

## Usage

**Layout**

    <com.eastwood.common.view.RefreshNestedLayout
        android:id="@+id/refresh_layout"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:refresh_header="@string/custom_header">
        
        <ListView
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
        
    </com.eastwood.common.view.RefreshNestedLayout>

**Set on OnRefreshListener**

    RefreshNestedLayout refreshLayout = (RefreshNestedLayout) findViewById(R.id.refresh_layout);
    refreshLayout.setOnRefreshListener(new RefreshNestedLayout.OnRefreshListener() {
        
        @Override
        public void onRefresh() {
            ...
            // call onRefreshComplete when the ListView's dataset has been refreshed.
            mRefreshLayout.onRefreshComplete();
        }
        
    });
    
    
**Custom your own refresh header**

    public class CustomHeaderLayout extends  RefreshHeaderLayout {
        ...
    }

**Global refresh header style**

    <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
        
        ...
        <item name="refresh_header">@string/custom_header</item>
        <item name="maxPullDistance">106dp</item>
        <item name="refreshingDistance">42dp</item>
        
    </style>
    
## License
```
   Copyright 2018 EastWood Yang

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

