package com.timappweb.timapp.listeners;

import android.support.v7.widget.SearchView;

import com.timappweb.timapp.Managers.SearchAndSelectTagManager;
import com.timappweb.timapp.adapters.DisplayedTagsAdapter;

/**
 * Created by stephane on 12/15/2015.
 */

public class OnQueryTagListener implements SearchView.OnQueryTextListener {

    private final SearchAndSelectTagManager manager;

    public OnQueryTagListener(SearchAndSelectTagManager searchAndSelectTagManager) {
        this.manager = searchAndSelectTagManager;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        DisplayedTagsAdapter adapter = (DisplayedTagsAdapter) manager.getSelectedTagsRecyclerView().getAdapter();
        adapter.addData(query);
        manager.getSelectedTagsRecyclerView().scrollToEnd();
        manager.getSearchView().setIconified(true);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        manager.suggestTag(newText);
        return false;
    }

}
