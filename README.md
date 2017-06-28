PageScrollView
==============

>PageScrollView widget that is a customized ViewGroup having function like `ScrollView` & `ViewPager` .***for more amusing customized ViewGroup please visit [WidgetLayout][3]***

**the sample using interaction image,both use a simple PageScrollView without nest another ViewGroup:**

![can't show scrollview style image][scrollview]
![can't show viewpager style image][viewpager]

**support basic function as below listed:**

* layout orientation either Horizontal or Vertical .
* scroll any child to its start or end position and ceiling there.
* work well with PageTransformer,PageChangeListener, ScrollChangeListener and VisibleRangeChangeListener;
* maxWidth&maxHeight,content gravity and child layout_gravity .
* smooth scroll any child to its start or centre with a optional offset and anim duration.

Why to Usage
============

  1. completely instead of ScrollView or HorizontalScrollView without nest a LinearLayout.
  2. scroll any child view to top or bottom and ceiling and float there .
  3. easy to listen on the visible index range change .
  4. using like a ViewPager but also support child with different width and height with selected item centre in parent.
  5. quite convenient to add a header or footer view to its edge.
  6. support setMaxWidth and setMaxHeight and can make all child to fill parent when measured not match parent size.
  
  so don't hesitate to use PageScrollView , it can support all above interaction requirement and more .
   
How to Usage
============

*For a working implementation of this project see the [`app/com.rexy`][1] folder.*

  1. Edit layout xml file , add attr properties to PageScrollView,then include any child widgets in it just like in LinearLayout.
       ``` xml
       <com.rexy.widget.PageScrollView                        
              android:id="@+id/pageScrollView"                
              android:layout_width="wrap_content"             
              android:layout_height="wrap_content" 
              android:layout_gravity="center" 
              android:minWidth="100dp"
              android:maxWidth="400dp"
              android:minHeight="100dp"
              android:maxHeight="900dp"
              android:orientation="horizontal" 
              android:gravity="center"                        
              rexy:childCenter="true"
              rexy:childFillParent="false" 
              rexy:floatViewEndIndex="-1"                          
              rexy:floatViewStartIndex="-1"                        
              rexy:middleMargin="10dp"                        
              rexy:overFlingDistance="0dp"                    
              rexy:viewPagerStyle="true"                       
              rexy:sizeFixedPercent="0">                      
              <include layout="@layout/merge_childs_layout" />
       </com.rexy.widget.PageScrollView>                      
       ```

  2.  *(Optional)* In your `onCreate` method (or `onCreateView` for a fragment), set support properties as above attr do.
      ``` java
      //set PageScrollView as you need to overwriting the attr properities.
      PageScrollView scrollView = (PageScrollView)findViewById(R.id.pageScrollView);
      //layout orientation HORIZONTAL or VERTICAL.
      scrollView.setOrientation(PageScrollView.VERTICAL); 
      
      //only ViewPager style it will scroll as ViewPager and OnPageChangeListener can be efficient
      scrollView.setViewPagerStyle(false);
      
      //each item measure fixed size for percent of parent width or height.
      scrollView.setSizeFixedPercent(0);
      
      //which item to fixed scroll to start or end [0,pageScrollView.getItemCount()-1],-1 to ignore.
      scrollView.setFloatViewStartIndex(0);
      scrollView.setFloatViewEndIndex(pageScrollView.getItemCount()-1);
      
      //force layout all its childs gravity as Gravity.CENTER.
      scrollView.setChildCenter(true);
      
      //if content size less than parent size , setChildFillParent as true to match parent size.
      scrollView.setChildFillParent(true);
            
      //set layout margin for each item between at the layout orientation.
      scrollView.setMiddleMargin(30);
      
      //set max width or height for this container.
      scrollview.setMaxWidth(400);
      scrollview.setMaxHeigh(800);
      ```

  3. *(Optional)* bind event for PageScrollView.
     ``` java
     //continued from above 
     scrollView.setPageHeadView(headerView);
     scrollView.setPageFooterView(footerView);
     // set PageTransform animation .
     scrollView.setPageTransformer(new PageScrollView.PageTransformer() {
         @Override
         public void transformPage(View view, float position, boolean horizontal) {
             //realized your transform animation for this view.
         }
         @Override
         public void recoverTransformPage(View view, boolean horizontal) {
             //clean your transform animation for this view.
         }
     });
     
     //set OnPageChangeListener like ViewPager.
     PageScrollView.OnPageChangeListener pagerScrollListener = new PageScrollView.OnPageChangeListener() {
         @Override
         public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
             //when selected item scroll from its center.
         }
         @Override
         public void onPageSelected(int position, int oldPosition) {
             // position current selected item ,oldPosition previous selected item
         }
         @Override
         public void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
             //when content scrolled,see View.onScrollChanged
         }
         @Override
         public void onScrollStateChanged(int state, int oldState) {
             // SCROLL_STATE_IDLE = 0; //scroll stopped .
             // SCROLL_STATE_DRAGGING = 1;//dragged scroll started .
             // SCROLL_STATE_SETTLING = 2;//fling scroll started .
         }
     };
     scrollView.setOnPageChangeListener(pagerScrollListener);
     // set OnScrollChangeListener to watch on onScrollChanged.
     scrollView.setOnScrollChangeListener(pagerScrollListener);
     
     // set OnVisibleRangeChangeListener  to watch on visible index range change
     scrollView.setOnVisibleRangeChangeListener(new OnVisibleRangeChangeListener(){
        public void onVisibleRangeChanged(int firstVisible, int lastVisible, int oldFirstVisible, int oldLastVisible){
        }
     });
     
     ```


More powerful expand PageScrollTab extends PageScrollView
====================


### `PageScrollTab` is a extended container from `PageScrollView` just like `SlidingTabStrip` . it support tab style behavior , easy combine together with ViewPager and work well.
 1. support attr properties , and can also use java api to set these properties.
    ``` xml
    <declare-styleable name="PageScrollTab">
        <!--tab item 的背景-->
        <attr name="tabItemBackground" format="reference"/>
        <attr name="tabItemBackgroundFirst" format="reference"/>
        <attr name="tabItemBackgroundLast" format="reference"/>
        <attr name="tabItemBackgroundFull" format="reference"/>
        <!--底部指示线-->
        <attr name="tabIndicatorColor" format="color"/>
        <attr name="tabIndicatorHeight" format="dimension"/>
        <attr name="tabIndicatorOffset" format="dimension"/>
        <attr name="tabIndicatorWidthPercent" format="float"/>
        <!--顶部水平分界线-->
        <attr name="tabTopLineColor" format="color"/>
        <attr name="tabTopLineHeight" format="dimension"/>
        <!--底部水平分界线-->
        <attr name="tabBottomLineColor" format="color"/>
        <attr name="tabBottomLineHeight" format="dimension"/>
        <!-- item 之间垂直分割线-->
        <attr name="tabItemDividerColor" format="color"/>
        <attr name="tabItemDividerWidth" format="dimension"/>
        <attr name="tabItemDividerPadding" format="dimension"/>
        <!-- item 的最小 Padding 设置-->
        <attr name="tabItemMinPaddingHorizontal" format="dimension"/>
        <attr name="tabItemMinPaddingTop" format="dimension"/>
        <attr name="tabItemMinPaddingBottom" format="dimension"/>
        <!--item文字大写开-->
        <attr name="tabItemTextCaps" format="boolean"/>
        <!--item 文字颜色-->
        <attr name="tabItemTextColor" format="reference"/>
    </declare-styleable>
    ```

2. use PageScrollTab

   PageScrollTab.setViewPager or setViewPager.setTabProvider ...


[download demo apk][2]
============

 [scrollview]:image/example_type_scrollview.gif "scrollview type but no need to nest a single ViewGroup,just use as a LinearLayout"
 [viewpager]:image/example_type_viewpager.gif  "viewpager type but not support PageAdapter"
 [1]:app/src/com/rexy/example/ExampleActivity.java "activity entry"
 [2]:image/PageScrollView.apk  "demo apk to download"
 [3]:https://github.com/rexyren/WidgetLayout
