<template>
  <MsCard simple no-content-padding>
    <div class="h-full p-[16px]">
      <MsAdvanceFilter
        v-model:keyword="keyword"
        :search-placeholder="t('caseManagement.featureCase.searchByNameAndId')"
        :filter-config-list="filterConfigList"
        :row-count="filterRowCount"
        @keyword-search="fetchData"
        @adv-search="handleAdvSearch"
        @refresh="handleAdvSearch"
      >
        <template #left>
          <div class="flex gap-[12px]">
            <a-button v-permission="['PROJECT_BUG:READ+ADD']" type="primary" @click="handleCreate"
              >{{ t('bugManagement.createBug') }}
            </a-button>
            <a-button
              v-if="currentPlatform !== 'Local'"
              v-permission="['PROJECT_BUG:READ+UPDATE']"
              :loading="!isComplete"
              type="outline"
              @click="handleSync"
              >{{ t('bugManagement.syncBug') }}
            </a-button>
          </div>
        </template>
      </MsAdvanceFilter>
      <MsBaseTable
        class="mt-[16px]"
        v-bind="propsRes"
        :action-config="tableBatchActions"
        v-on="propsEvent"
        @batch-action="handleTableBatch"
        @sorter-change="saveSort"
      >
        <!-- ID -->
        <template #num="{ record, rowIndex }">
          <a-button type="text" class="px-0 text-[14px] leading-[22px]" @click="handleShowDetail(record.id, rowIndex)">
            {{ record.num }}
          </a-button>
        </template>
        <template #operation="{ record }">
          <div class="flex flex-nowrap items-center">
            <span v-permission="['PROJECT_BUG:READ+UPDATE']" class="flex flex-row items-center">
              <MsButton class="!mr-0" :disabled="currentPlatform !== record.platform" @click="handleEdit(record)">
                {{ t('common.edit') }}
              </MsButton>
              <a-divider class="!mx-2 h-[12px]" direction="vertical" />
            </span>
            <span v-permission="['PROJECT_BUG:READ+ADD']" class="flex flex-row items-center">
              <MsButton class="!mr-0" :disabled="currentPlatform !== record.platform" @click="handleCopy(record)">
                {{ t('common.copy') }}
              </MsButton>
              <a-divider class="!mx-2 h-[12px]" direction="vertical" />
            </span>
            <MsTableMoreAction
              v-permission="['PROJECT_BUG:READ+DELETE']"
              :list="moreActionList"
              trigger="click"
              @select="handleMoreActionSelect($event, record)"
            />
          </div>
        </template>
        <template #relationCaseCount="{ record, rowIndex }">
          <a-button type="text" class="px-0" @click="showDetail(record.id, rowIndex, 'case')">
            {{ record.relationCaseCount }}
          </a-button>
        </template>
        <template #statusName="{ record }">
          {{ record.statusName || '-' }}
        </template>
      </MsBaseTable>
    </div>
  </MsCard>
  <a-modal
    v-model:visible="syncVisible"
    title-align="start"
    class="ms-modal-form ms-modal-small"
    :ok-text="t('bugManagement.sync')"
    unmount-on-close
    @cancel="handleSyncCancel()"
    @ok="handleSyncModalOk"
  >
    <template #title>
      <div class="flex flex-row items-center gap-[4px]">
        <div class="medium text-[var(--color-text-1)]">{{ t('bugManagement.syncBug') }} </div>
        <a-tooltip position="top">
          <template #content>
            <div>{{ t('bugManagement.syncBugTipRowOne') }}</div>
          </template>
          <MsIcon class="text-[var(--color-text-4)]" type="icon-icon-maybe_outlined" />
        </a-tooltip>
      </div>
    </template>
    <div
      class="flex flex-row items-center gap-[8px] rounded-[4px] border-[1px] border-[rgb(var(--primary-5))] bg-[rgb(var(--primary-1))] px-[16px] py-[12px]"
    >
      <icon-exclamation-circle-fill class="text-[rgb(var(--primary-5))]" />
      <div>{{ t('bugManagement.bugAutoSync') }}</div>
    </div>
    <div class="mb-[8px] mt-[16px]">{{ t('bugManagement.createTime') }}</div>
    <div class="flex flex-row gap-[8px]">
      <a-select v-model="syncObject.operator" class="w-[120px]">
        <a-option
          v-for="option in timeSelectOptions"
          :key="option.label"
          :label="t(option.label)"
          :value="option.value"
        />
      </a-select>
      <a-date-picker v-model="syncObject.time" value-format="timestamp" show-time class="w-[304px]" />
    </div>
  </a-modal>
  <MsExportDrawer
    v-model:visible="exportVisible"
    :export-loading="exportLoading"
    :all-data="exportOptionData"
    @confirm="exportConfirm"
  >
    <template #title>
      <span class="text-[var(--color-text-1)]">{{ t('bugManagement.exportBug') }}</span>
      <span v-if="currentSelectParams.currentSelectCount" class="text-[var(--color-text-4)]"
        >({{ t('bugManagement.exportBugCount', { count: currentSelectParams.currentSelectCount }) }})</span
      >
    </template>
  </MsExportDrawer>
  <BugDetailDrawer
    v-model:visible="detailVisible"
    :detail-id="activeDetailId"
    :detail-index="activeCaseIndex"
    :detail-default-tab="activeDetailTab"
    :current-platform="currentPlatform"
    :table-data="propsRes.data"
    :page-change="propsEvent.pageChange"
    :pagination="propsRes.msPagination!"
    @submit="fetchData"
  />
  <DeleteModal
    :id="currentDeleteObj.id"
    v-model:visible="deleteVisible"
    :name="currentDeleteObj.title"
    :remote-func="deleteSingleBug"
    @submit="handleSingleDelete"
  />
  <BatchEditModal
    v-model:visible="batchEditVisible"
    :select-param="currentSelectParams"
    :custom-fields="customFields"
    @submit="batchEditConfirm"
  />
</template>

<script lang="ts" async setup>
  import { ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { useIntervalFn } from '@vueuse/core';
  import { Message, TableData } from '@arco-design/web-vue';
  import { cloneDeep } from 'lodash-es';

  import { MsAdvanceFilter, timeSelectOptions } from '@/components/pure/ms-advance-filter';
  import { BackEndEnum, FilterFormItem, FilterResult, FilterType } from '@/components/pure/ms-advance-filter/type';
  import MsButton from '@/components/pure/ms-button/index.vue';
  import MsCard from '@/components/pure/ms-card/index.vue';
  import { MsExportDrawerMap, MsExportDrawerOption } from '@/components/pure/ms-export-drawer/types';
  import { BatchActionParams, BatchActionQueryParams, MsTableColumn } from '@/components/pure/ms-table/type';
  import useTable from '@/components/pure/ms-table/useTable';
  import MsTableMoreAction from '@/components/pure/ms-table-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/ms-table-more-action/types';
  import BugDetailDrawer from './components/bug-detail-drawer.vue';

  import {
    checkBugExist,
    deleteBatchBug,
    deleteSingleBug,
    exportBug,
    getBugList,
    getCustomFieldHeader,
    getCustomOptionHeader,
    getExportConfig,
    getPlatform,
    getSyncStatus,
    syncBugEnterprise,
  } from '@/api/modules/bug-management';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import router from '@/router';
  import { useAppStore, useTableStore } from '@/store';
  import useLicenseStore from '@/store/modules/setting/license';
  import { customFieldDataToTableData, customFieldToColumns, downloadByteFile } from '@/utils';
  import { hasAnyPermission } from '@/utils/permission';

  import { BugEditCustomField, BugListItem, BugOptionItem } from '@/models/bug-management';
  import { RouteEnum } from '@/enums/routeEnum';
  import { TableKeyEnum } from '@/enums/tableEnum';

  import { makeColumns } from '@/views/case-management/caseManagementFeature/components/utils';

  const { t } = useI18n();
  const MsExportDrawer = defineAsyncComponent(() => import('@/components/pure/ms-export-drawer/index.vue'));
  const DeleteModal = defineAsyncComponent(() => import('./components/deleteModal.vue'));
  const MsBaseTable = defineAsyncComponent(() => import('@/components/pure/ms-table/base-table.vue'));
  const BatchEditModal = defineAsyncComponent(() => import('./components/batchEditModal.vue'));

  const tableStore = useTableStore();
  const appStore = useAppStore();
  const projectId = computed(() => appStore.currentProjectId);
  const filterVisible = ref(false);
  const filterRowCount = ref(0);
  const syncVisible = ref(false);
  const exportVisible = ref(false);
  const exportOptionData = ref<MsExportDrawerMap>({});
  const exportLoading = ref(false);
  const currentPlatform = ref('Local');
  const detailVisible = ref(false);
  const activeDetailId = ref<string>('');
  const activeCaseIndex = ref<number>(0);
  const activeDetailTab = ref<string>('');
  const currentDeleteObj = reactive<{ id: string; title: string }>({ id: '', title: '' });
  const deleteVisible = ref(false);
  const batchEditVisible = ref(false);
  const keyword = ref('');
  const filterResult = ref<FilterResult>({ accordBelow: 'AND', combine: {} });
  const licenseStore = useLicenseStore();
  const isXpack = computed(() => licenseStore.hasLicense());
  const { openDeleteModal } = useModal();
  const route = useRoute();
  const severityFilterOptions = ref<BugOptionItem[]>([]);

  // 是否同步完成
  const isComplete = ref(false);
  const isShowCompleteMsg = ref(false);
  // 自定义字段
  const customFields = ref<BugEditCustomField[]>([]);
  // 当前选择的条数
  const currentSelectParams = ref<BatchActionQueryParams>({ selectAll: false, currentSelectCount: 0 });
  // 排序
  const sort = ref<{ [key: string]: string }>({});

  const syncObject = reactive({
    time: 0,
    operator: '',
  });
  const handleSyncCancel = () => {
    syncVisible.value = false;
  };
  const filterConfigList = reactive<FilterFormItem[]>([
    {
      title: 'bugManagement.ID',
      dataIndex: 'num',
      type: FilterType.INPUT,
      backendType: BackEndEnum.NUMBER,
    },
    {
      title: 'bugManagement.bugName',
      dataIndex: 'title',
      type: FilterType.INPUT,
      backendType: BackEndEnum.STRING,
    },
    {
      title: 'bugManagement.createTime',
      dataIndex: 'createTime',
      type: FilterType.DATE_PICKER,
    },
  ]);

  const heightUsed = computed(() => 286 + (filterVisible.value ? 160 + (filterRowCount.value - 1) * 60 : 0));

  // 获取自定义字段
  const getCustomFieldColumns = async () => {
    const res = await getCustomFieldHeader(projectId.value);
    customFields.value = res;

    //  实例化自定义字段的filters
    customFields.value.forEach((item) => {
      if ((item.fieldName === '严重程度' || item.fieldName === 'Bug Degree') && item.options) {
        item.options.forEach((option) => {
          severityFilterOptions.value.push({
            value: option.value,
            text: option.text,
          });
        });
      }
    });

    return customFieldToColumns(res);
  };

  let columns: MsTableColumn = [
    {
      title: 'bugManagement.ID',
      dataIndex: 'num',
      slotName: 'num',
      width: 100,
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      showInTable: true,
      columnSelectorDisabled: true,
    },
    {
      title: 'bugManagement.bugName',
      dataIndex: 'title',
      width: 250,
      showTooltip: true,
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      showInTable: true,
      columnSelectorDisabled: true,
    },
    {
      title: 'bugManagement.status',
      dataIndex: 'status',
      width: 100,
      showTooltip: true,
      slotName: 'statusName',
      filterConfig: {
        options: [],
        labelKey: 'text',
      },
      showDrag: true,
      showInTable: true,
    },
    {
      title: 'bugManagement.handleMan',
      dataIndex: 'handleUser',
      slotName: 'handleUser',
      showTooltip: true,
      width: 125,
      filterConfig: {
        options: [],
        labelKey: 'text',
      },
      showDrag: true,
      showInTable: true,
    },
    {
      title: 'bugManagement.numberOfCase',
      dataIndex: 'relationCaseCount',
      slotName: 'relationCaseCount',
      width: 80,
      showDrag: true,
      showInTable: true,
    },
    {
      title: 'bugManagement.belongPlatform',
      width: 100,
      showDrag: true,
      dataIndex: 'platform',
      showInTable: true,
    },
    {
      title: 'bugManagement.tag',
      showDrag: true,
      isStringTag: true,
      dataIndex: 'tags',
      showInTable: true,
    },
    {
      title: 'bugManagement.creator',
      dataIndex: 'createUser',
      slotName: 'createUser',
      width: 125,
      showTooltip: true,
      showDrag: true,
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      filterConfig: {
        options: [],
        labelKey: 'text',
      },
      showInTable: true,
    },
    {
      title: 'bugManagement.createTime',
      dataIndex: 'createTime',
      showDrag: true,
      width: 199,
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      showInTable: true,
    },
    {
      title: 'bugManagement.updateUser',
      dataIndex: 'updateUser',
      width: 125,
      showTooltip: true,
      showDrag: true,
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      filterConfig: {
        options: [],
        labelKey: 'text',
      },
      showInTable: true,
    },
    {
      title: 'bugManagement.updateTime',
      dataIndex: 'updateTime',
      showDrag: true,
      width: 199,
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      showInTable: true,
    },
    {
      title: 'common.operation',
      slotName: 'operation',
      dataIndex: 'operation',
      fixed: 'right',
      width: 140,
    },
  ];

  const { propsRes, propsEvent, setKeyword, setAdvanceFilter, setLoadListParams, setProps, resetSelector, loadList } =
    useTable(
      getBugList,
      {
        tableKey: TableKeyEnum.BUG_MANAGEMENT,
        selectable: true,
        noDisable: false,
        showSetting: true,
        heightUsed: 106,
        paginationSize: 'mini',
      },
      (record: TableData) => ({
        ...record,
        handleUser: record.handleUserName,
        createUser: record.createUserName,
        updateUser: record.updateUserName,
        ...customFieldDataToTableData(record.customFields, customFields.value),
      })
    );

  const tableBatchActions = {
    baseAction: [
      {
        label: 'common.export',
        eventTag: 'export',
        permission: ['PROJECT_BUG:READ+EXPORT'],
      },
      {
        label: 'common.edit',
        eventTag: 'edit',
        permission: ['PROJECT_BUG:READ+UPDATE'],
      },
      {
        label: 'common.delete',
        eventTag: 'delete',
        danger: true,
        permission: ['PROJECT_BUG:READ+DELETE'],
      },
    ],
  };

  function initTableParams() {
    return {
      keyword: keyword.value,
      projectId: projectId.value,
      condition: {
        keyword: keyword.value,
        filter: propsRes.value.filter,
      },
    };
  }

  function searchData() {
    setLoadListParams(initTableParams());
    loadList();
  }

  const fetchData = async (v = '') => {
    setKeyword(v);
    searchData();
  };

  const handleAdvSearch = (filter: FilterResult) => {
    filterResult.value = filter;
    const { accordBelow, combine } = filter;
    setAdvanceFilter(filter);
    currentSelectParams.value = {
      ...currentSelectParams.value,
      condition: {
        keyword: keyword.value,
        searchMode: accordBelow,
        filter: propsRes.value.filter,
        combine,
      },
    };
    fetchData();
  };

  const exportConfirm = async (option: MsExportDrawerOption[]) => {
    try {
      exportLoading.value = true;
      const blob = await exportBug({
        ...currentSelectParams.value,
        exportColumns: option.map((item) => item),
        projectId: appStore.currentProjectId,
        exportSort: sort.value,
      });
      downloadByteFile(blob, `${t('bugManagement.exportBug')}.zip`);
      exportLoading.value = false;
      exportVisible.value = false;
      resetSelector();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  };

  const handleSingleDelete = (record?: TableData) => {
    if (record) {
      currentDeleteObj.id = record.id;
      currentDeleteObj.title = record.title;
      deleteVisible.value = true;
    } else {
      fetchData();
      currentDeleteObj.id = '';
      currentDeleteObj.title = '';
    }
  };

  const batchEditConfirm = () => {
    resetSelector();
    fetchData();
  };

  const handleCreate = () => {
    router.push({
      name: RouteEnum.BUG_MANAGEMENT_DETAIL,
      params: {
        mode: 'add',
      },
    });
  };

  const checkSyncStatus = async () => {
    if (!hasAnyPermission(['PROJECT_BUG:READ+UPDATE'])) {
      return;
    }
    try {
      const { complete } = await getSyncStatus(appStore.currentProjectId);
      isComplete.value = complete;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  };
  /** 同步缺陷 */
  const { pause, resume } = useIntervalFn(() => {
    isShowCompleteMsg.value = true;
    checkSyncStatus();
  }, 1000);
  // 初始化组件时关闭
  pause();
  watchEffect(() => {
    if (isComplete.value) {
      if (isShowCompleteMsg.value) {
        isShowCompleteMsg.value = false;
        Message.success(t('bugManagement.syncSuccess'));
        fetchData();
      }
      // 同步完成时关闭轮询
      pause();
    }
  });
  // 同步缺陷按钮触发
  const handleSync = async () => {
    syncVisible.value = true;
  };
  const handleSyncModalOk = async () => {
    try {
      await syncBugEnterprise({
        projectId: appStore.currentProjectId,
        pre: syncObject.operator === 'LT_OR_EQUALS',
        createTime: syncObject.time,
      });
      Message.warning(t('bugManagement.synchronizing'));
      isComplete.value = false;
      isShowCompleteMsg.value = true;
      // 开始轮询
      resume();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  };

  const checkBug = async (id: string) => {
    const res = await checkBugExist(id);
    return res;
  };

  const showDetail = (id: string, rowIndex: number, tab: string) => {
    activeDetailId.value = id;
    activeCaseIndex.value = rowIndex;
    activeDetailTab.value = tab;
    detailVisible.value = true;
  };

  const handleShowDetail = async (id: string, rowIndex: number) => {
    const exist = await checkBug(id);
    if (!exist) {
      // 缺陷不存在, 不展示详情
      Message.error(t('bugManagement.detail.notExist'));
      const query = { ...route.query };
      delete query.id;
      await router.push({
        name: RouteEnum.BUG_MANAGEMENT_INDEX,
        query,
      });
    } else {
      showDetail(id, rowIndex, 'detail');
    }
  };

  const handleCopy = (record: BugListItem) => {
    router.push({
      name: RouteEnum.BUG_MANAGEMENT_DETAIL,
      query: {
        id: record.id,
      },
      params: {
        mode: 'copy',
      },
    });
  };

  const handleEdit = (record: BugListItem) => {
    router.push({
      name: RouteEnum.BUG_MANAGEMENT_DETAIL,
      query: {
        id: record.id,
      },
      params: {
        mode: 'edit',
      },
    });
  };

  const handleBatchDelete = () => {
    let dataCount = currentSelectParams.value.selectIds?.length;
    if (currentSelectParams.value.selectAll) {
      dataCount = currentSelectParams.value.currentSelectCount;
    }
    openDeleteModal({
      title: t('bugManagement.deleteCount', { count: dataCount }),
      content: t('bugManagement.deleteTip'),
      onBeforeOk: async () => {
        try {
          const tmpObj = {
            ...currentSelectParams.value,
            projectId: projectId.value,
          };
          await deleteBatchBug(tmpObj);
          Message.success(t('common.deleteSuccess'));
          resetSelector();
          fetchData();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
    });
  };

  const handleBatchEdit = () => {
    batchEditVisible.value = true;
  };

  const handleExport = () => {
    exportVisible.value = true;
  };

  const setExportOptionData = async () => {
    const res = await getExportConfig(projectId.value);
    exportOptionData.value = res;
  };

  const setCurrentPlatform = async () => {
    const res = await getPlatform(projectId.value);
    currentPlatform.value = res;
  };

  const moreActionList: ActionsItem[] = [
    {
      label: t('common.delete'),
      danger: true,
      eventTag: 'delete',
    },
  ];

  function handleMoreActionSelect(item: ActionsItem, record: BugListItem) {
    if (item.eventTag === 'delete') {
      handleSingleDelete(record);
    }
  }

  function handleTableBatch(event: BatchActionParams, params: BatchActionQueryParams) {
    if (params.condition) {
      params.condition.filter = propsRes.value.filter;
    } else {
      params.condition = { filter: propsRes.value.filter };
    }
    const condition = {
      keyword: keyword.value,
      searchMode: filterResult.value.accordBelow,
      filter: propsRes.value.filter,
      combine: filterResult.value.combine,
    };
    currentSelectParams.value = {
      excludeIds: params.excludeIds,
      selectAll: params.selectAll,
      selectIds: params.selectedIds,
      currentSelectCount: params.currentSelectCount,
      condition,
    };

    switch (event.eventTag) {
      case 'export':
        handleExport();
        break;
      case 'delete':
        handleBatchDelete();
        break;
      case 'edit':
        handleBatchEdit();
        break;
      default:
        break;
    }
  }

  async function initFilterOptions() {
    const res = await getCustomOptionHeader(appStore.currentProjectId);
    const filterOptionsMaps: Record<string, any> = {
      status: res.statusOption,
      handleUser: res.handleUserOption,
      createUser: res.userOption,
      updateUser: res.userOption,
    };

    columns = makeColumns(filterOptionsMaps, columns);
  }

  function saveSort(sortObj: { [key: string]: string }) {
    sort.value = sortObj;
  }

  watchEffect(() => {
    setProps({ heightUsed: heightUsed.value });
  });

  onBeforeMount(() => {
    // 进入页面时检查当前项目轮训状态
    checkSyncStatus();
  });

  let customColumns: MsTableColumn = [];
  async function getColumnHeaders() {
    try {
      customColumns = await getCustomFieldColumns();
      customColumns.forEach((item) => {
        if (item.title === '严重程度' || item.title === 'Bug Degree') {
          item.showInTable = true;
          item.slotName = 'severity';
          item.filterConfig = {
            options: cloneDeep(unref(severityFilterOptions.value)) || [],
            labelKey: 'text',
          };
        } else {
          item.showInTable = false;
        }
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  await getColumnHeaders();
  await initFilterOptions();
  await tableStore.initColumn(TableKeyEnum.BUG_MANAGEMENT, columns.concat(customColumns), 'drawer');

  onMounted(() => {
    setLoadListParams({ projectId: projectId.value });
    setCurrentPlatform();
    setExportOptionData();
    fetchData();
    if (route.query.id) {
      // 分享或成功进来的页面
      handleShowDetail(route.query.id as string, -1);
    }
  });

  onBeforeUnmount(() => {
    detailVisible.value = false;
  });

  onUnmounted(() => {
    // 组件销毁时关闭轮询
    pause();
  });
</script>

<style lang="less" scoped>
  :deep(.arco-divider-vertical) {
    margin: 0 8px;
  }
</style>
