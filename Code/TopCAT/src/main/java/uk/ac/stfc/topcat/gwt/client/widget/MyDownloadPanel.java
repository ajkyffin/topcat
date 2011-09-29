/**
 * 
 * Copyright (c) 2009-2010
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * Neither the name of the STFC nor the names of its contributors may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY 
 * OF SUCH DAMAGE.
 */
package uk.ac.stfc.topcat.gwt.client.widget;

/**
 * Imports
 */
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Orientation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import uk.ac.stfc.topcat.gwt.client.UtilityService;
import uk.ac.stfc.topcat.gwt.client.UtilityServiceAsync;
import uk.ac.stfc.topcat.gwt.client.callback.EventPipeLine;
import uk.ac.stfc.topcat.gwt.client.model.DownloadModel;

import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.PagingLoader;
import com.extjs.gxt.ui.client.data.PagingModelMemoryProxy;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.BoxComponent;
import com.extjs.gxt.ui.client.widget.Composite;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;

/**
 * This widget displays the download requests.
 * 
 */
public class MyDownloadPanel extends Composite {
    private final UtilityServiceAsync utilityService = GWT.create(UtilityService.class);
    private Grid<DownloadModel> grid;
    private PagingModelMemoryProxy proxy = new PagingModelMemoryProxy(new ArrayList<DownloadModel>());
    private PagingToolBar toolBar = null;
    private EventPipeLine eventBus;
    private WaitDialog waitDialog;

    public MyDownloadPanel() {

        GridCellRenderer<DownloadModel> buttonRenderer = new GridCellRenderer<DownloadModel>() {

            private boolean init;

            public Object render(final DownloadModel model, String property, ColumnData config, final int rowIndex,
                    final int colIndex, ListStore<DownloadModel> store, Grid<DownloadModel> grid) {
                if (!init) {
                    init = true;
                    grid.addListener(Events.ColumnResize, new Listener<GridEvent<DownloadModel>>() {

                        public void handleEvent(GridEvent<DownloadModel> be) {
                            for (int i = 0; i < be.getGrid().getStore().getCount(); i++) {
                                if (be.getGrid().getView().getWidget(i, be.getColIndex()) != null
                                        && be.getGrid().getView().getWidget(i, be.getColIndex()) instanceof BoxComponent) {
                                    ((BoxComponent) be.getGrid().getView().getWidget(i, be.getColIndex())).setWidth(be
                                            .getWidth() - 10);
                                }
                            }
                        }
                    });
                }

                Button b = new Button((String) model.get(property), new SelectionListener<ButtonEvent>() {
                    @Override
                    public void componentSelected(ButtonEvent ce) {
                        if (model.getStatus().equalsIgnoreCase("success")) {
                            eventBus.download(model.getUrl());
                        }
                    }
                });
                if (model.getStatus().equalsIgnoreCase("success")) {
                    b.setEnabled(true);
                    b.setText("re-download");
                } else if (model.getStatus().equalsIgnoreCase("expired")) {
                    b.setEnabled(false);
                    b.setText("expired");
                } else {
                    b.setEnabled(false);
                    b.setText("waiting");
                }
                b.setWidth(grid.getColumnModel().getColumnWidth(colIndex) - 10);
                b.setToolTip("Click to start re-download");
                return b;
            }
        };

        waitDialog = new WaitDialog();

        ContentPanel contentPanel = new ContentPanel();
        contentPanel.setHeaderVisible(false);
        contentPanel.setCollapsible(true);
        contentPanel.setLayout(new RowLayout(Orientation.VERTICAL));

        VerticalPanel bodyPanel = new VerticalPanel();
        bodyPanel.setHorizontalAlign(HorizontalAlignment.CENTER);

        List<ColumnConfig> configs = new ArrayList<ColumnConfig>();

        ColumnConfig column;
        column = new ColumnConfig("facilityName", "Facility Name", 150);
        configs.add(column);

        column = new ColumnConfig("submitTime", "Submit Time", 150);
        column.setDateTimeFormat(DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.DATE_TIME_SHORT));
        configs.add(column);

        column = new ColumnConfig("downloadName", "Download Name", 150);
        configs.add(column);

        column = new ColumnConfig("status", "Status", 150);
        configs.add(column);

        column = new ColumnConfig("timeRemaining", "Download Availability", 200);
        configs.add(column);

        column = new ColumnConfig("downloadAvailable", "Download", 100);
        column.setRenderer(buttonRenderer);
        configs.add(column);

        // Pagination
        PagingLoader<PagingLoadResult<DownloadModel>> loader = new BasePagingLoader<PagingLoadResult<DownloadModel>>(
                proxy);
        loader.setRemoteSort(true);
        ListStore<DownloadModel> store = new ListStore<DownloadModel>(loader);

        grid = new Grid<DownloadModel>(store, new ColumnModel(configs));
        grid.setAutoExpandColumn("downloadName");
        grid.setAutoExpandMin(200);
        grid.setMinColumnWidth(100);
        grid.setHeight("490px");
        bodyPanel.add(grid);
        contentPanel.add(bodyPanel);

        // Pagination Bar
        toolBar = new PagingToolBar(15) {
            @Override
            public void refresh() {
                refreshDownloadData();
                super.refresh();
            }
        };
        toolBar.bind(loader);
        contentPanel.setBottomComponent(toolBar);

        setMonitorWindowResize(true);
        initComponent(contentPanel);
    }

    /**
     * Add details of a download.
     * 
     * @param dlm
     *            a DownloadModel
     */
    public void addDownload(DownloadModel dlm) {
        @SuppressWarnings("unchecked")
        List<DownloadModel> downloadList = (List<DownloadModel>) proxy.getData();
        if (downloadList == null) {
            downloadList = new ArrayList<DownloadModel>();
        }
        downloadList.add(dlm);
        proxy.setData(downloadList);
        toolBar.refresh();
    }

    /**
     * Add a list of downloads.
     * 
     * @param dlms
     *            a list of DownloadModels
     */
    public void addDownloads(List<DownloadModel> dlms) {
        loadDownloads(dlms);
        toolBar.refresh();
    }

    /**
     * Remove all downloads for the given facility.
     * 
     * @param facilityName
     */
    public void clearDownloads(String facilityName) {
        clearDownloadList(facilityName);
        toolBar.refresh();
    }

    public void setEventBus(EventPipeLine eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * This method sets the width of the my downloads table.
     * 
     * @param width
     */
    public void setGridWidth(int width) {
        grid.setWidth(width);
    }

    /**
     * Remove all downloads for the given facility.
     * 
     * @param facilityName
     */
    private void clearDownloadList(String facilityName) {
        @SuppressWarnings("unchecked")
        List<DownloadModel> downloadList = (List<DownloadModel>) proxy.getData();
        if (downloadList != null) {
            for (Iterator<DownloadModel> it = downloadList.iterator(); it.hasNext();) {
                if (it.next().getFacilityName().equals(facilityName)) {
                    it.remove();
                }
            }
            proxy.setData(downloadList);
        }
    }

    private void loadDownloads(List<DownloadModel> dlms) {
        @SuppressWarnings("unchecked")
        List<DownloadModel> downloadList = (List<DownloadModel>) proxy.getData();
        if (downloadList == null) {
            downloadList = new ArrayList<DownloadModel>();
        }
        downloadList.addAll(dlms);
        proxy.setData(downloadList);
    }

    /**
     * Only call out to check status if any of the model's status are not
     * 'downloaded'. If all the status are 'downloaded' then update the ttl
     * locally.
     */
    private void refreshDownloadData() {
        @SuppressWarnings("unchecked")
        List<DownloadModel> downloadList = (List<DownloadModel>) proxy.getData();
        if (downloadList != null) {
            Set<String> facilities = new HashSet<String>();
            for (Iterator<DownloadModel> it = downloadList.iterator(); it.hasNext();) {
                DownloadModel model = it.next();
                if (model.getStatus().equals("in progress")) {
                    facilities.add(model.getFacilityName());
                }
                model.refresh();
            }
            if (facilities.isEmpty()) {
                proxy.setData(downloadList);
            } else {
                remoteRefresh(facilities);
            }
        }
    }

    /**
     * Refresh all data for the given facilities.
     * 
     * @param facilities
     */
    private void remoteRefresh(final Set<String> facilities) {
        waitDialog.setMessage("  Refreshing data...");
        waitDialog.show();
        utilityService.getMyDownloadList(facilities, new AsyncCallback<ArrayList<DownloadModel>>() {
            @Override
            public void onSuccess(ArrayList<DownloadModel> result) {
                for (String facility : facilities) {
                    clearDownloadList(facility);
                }
                loadDownloads(result);
                waitDialog.hide();
            }

            @Override
            public void onFailure(Throwable caught) {
                waitDialog.hide();
                EventPipeLine.getInstance().showMessageDialog("Error retrieving fresh data from server");
            }
        });
    }

}
