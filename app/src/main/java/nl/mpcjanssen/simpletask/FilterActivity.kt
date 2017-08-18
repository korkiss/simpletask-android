package nl.mpcjanssen.simpletask

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.getString
import java.util.*

class FilterActivity : ThemedNoActionBarActivity() {

    internal lateinit var mFilter: ActiveFilter

    internal lateinit var m_app: TodoApplication
    val prefs = Config.prefs

    private var pager: ViewPager? = null
    private var m_menu: Menu? = null
    private var pagerAdapter: ScreenSlidePagerAdapter? = null
    private var m_page = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Called with intent: " + intent.toString())
        m_app = application as TodoApplication

        setContentView(R.layout.filter)
        val toolbar = findViewById(R.id.toolbar_edit_filter) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)

        val intent = intent

        mFilter = ActiveFilter()
        mFilter.initFromIntent(intent)


        pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)


        // Fill arguments for fragment
        var arguments = Bundle()
        arguments.putStringArrayList(FILTER_ITEMS, mFilter.getSort(Config.defaultSorts))
        arguments.putString(TAB_TYPE, SORT_TAB)
        val sortTab = FilterSortFragment()
        sortTab.arguments = arguments
        pagerAdapter!!.add(sortTab)

        // Fill arguments for fragment
        arguments = Bundle()
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_COMPLETED_FILTER, mFilter.hideCompleted)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_FUTURE_FILTER, mFilter.hideFuture)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_LISTS_FILTER, mFilter.hideLists)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_TAGS_FILTER, mFilter.hideTags)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_CREATE_DATE_FILTER, mFilter.hideCreateDate)
        arguments.putBoolean(ActiveFilter.INTENT_HIDE_HIDDEN_FILTER, mFilter.hideHidden)
        arguments.putBoolean(ActiveFilter.INTENT_CREATE_AS_THRESHOLD, mFilter.createIsThreshold)
        arguments.putString(TAB_TYPE, OTHER_TAB)
        val otherTab = FilterOtherFragment()
        otherTab.arguments = arguments
        pagerAdapter!!.add(otherTab)



        pager = findViewById(R.id.pager) as ViewPager
        pager!!.adapter = pagerAdapter
        // Give the TabLayout the ViewPager
        val tabLayout = findViewById(R.id.sliding_tabs) as TabLayout
        tabLayout.setupWithViewPager(pager as ViewPager)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        pager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                return
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                return
            }

            override fun onPageSelected(position: Int) {
                Log.i(TAG, "Page $position selected")
                m_page = position
            }
        })
        val activePage = prefs.getInt(getString(R.string.last_open_filter_tab), 0)
        if (activePage < pagerAdapter?.count ?: 0) {
            pager?.setCurrentItem(activePage, false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        val inflater = menuInflater
        inflater.inflate(R.menu.filter, menu)
        m_menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_filter_action -> applyFilter()
        }
        return true
    }

    private fun createFilterIntent(): Intent {
        val target = Intent(this, Simpletask::class.java)
        target.action = Constants.INTENT_START_FILTER
        updateFilterFromFragments()
        mFilter.name = mFilter.proposedName
        mFilter.saveInIntent(target)

        target.putExtra("name", mFilter.proposedName)
        return target
    }

    private fun updateFilterFromFragments() {
        for (f in pagerAdapter!!.fragments) {
            when (f.arguments.getString(TAB_TYPE, "")) {
                "" -> {
                }
                OTHER_TAB -> {
                    val of = f as FilterOtherFragment
                    mFilter.hideCompleted = of.hideCompleted
                    mFilter.hideFuture = of.hideFuture
                    mFilter.hideLists = of.hideLists
                    mFilter.hideTags = of.hideTags
                    mFilter.hideCreateDate = of.hideCreateDate
                    mFilter.hideHidden = of.hideHidden
                    mFilter.createIsThreshold = of.createAsThreshold
                }
                SORT_TAB -> {
                    val sf = f as FilterSortFragment
                    mFilter.setSort(sf.selectedItem)
                }

            }
        }
    }

    private fun applyFilter() {
        val data = createFilterIntent()
        startActivity(data)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.edit().putInt(getString(R.string.last_open_filter_tab), m_page).apply()
        pager?.clearOnPageChangeListeners()
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        val fragments: ArrayList<Fragment> = ArrayList<Fragment>()

        fun add(frag: Fragment) {
            fragments.add(frag)
        }

        override fun getPageTitle(position: Int): CharSequence {
            val f = fragments[position]
            val type = f.arguments.getString(TAB_TYPE, "unknown")
            return type
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

    }

    companion object {

        val TAG = "FilterActivity"
        val TAB_TYPE = "type"

        val OTHER_TAB = getString(R.string.filter_tab_header_other)
        val SORT_TAB = getString(R.string.filter_tab_header_sort)

        // Constants for saving state
        val FILTER_ITEMS = "items"
    }
}

