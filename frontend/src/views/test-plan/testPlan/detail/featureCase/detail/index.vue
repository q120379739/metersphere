<template>
  <MsCard :min-width="1100" has-breadcrumb simple no-content-padding>
    <div class="flex h-full w-full">
      <!-- 左侧 -->
      <div class="flex h-full w-[318px] flex-col border-r border-[var(--color-text-n8)] p-[16px]">
        <a-tooltip :content="`[${planDetail.num}]${planDetail.name}`">
          <div class="one-line-text w-full gap-[4px] font-medium">
            <span>[{{ planDetail.num }}]</span>
            {{ planDetail.name }}
          </div>
        </a-tooltip>
        <div class="my-[8px] flex">
          <a-input-search
            v-model:model-value="keyword"
            :placeholder="t('caseManagement.caseReview.searchPlaceholder')"
            allow-clear
            class="mr-[8px] w-[176px]"
            @search="loadCaseList"
            @press-enter="loadCaseList"
            @clear="loadCaseList"
          />
          <a-select
            v-model:model-value="lastExecResult"
            :options="executeResultOptions"
            class="flex-1"
            @change="loadCaseList"
          >
          </a-select>
        </div>
        <a-spin :loading="caseListLoading" class="w-full flex-1 overflow-hidden">
          <div class="case-list">
            <div
              v-for="item of caseList"
              :key="item.id"
              :class="['case-item', activeId === item.id ? 'case-item--active' : '']"
              @click="changeActiveCase(item)"
            >
              <div class="mb-[8px] flex items-center justify-between">
                <div class="text-[var(--color-text-4)]">{{ item.num }}</div>
                <ExecuteResult :execute-result="item.lastExecResult ?? LastExecuteResults.PENDING" />
              </div>
              <a-tooltip :content="item.name">
                <div class="one-line-text">{{ item.name }}</div>
              </a-tooltip>
            </div>
            <MsEmpty v-if="caseList.length === 0" />
          </div>
          <MsPagination
            v-model:page-size="pageNation.pageSize"
            v-model:current="pageNation.current"
            :total="pageNation.total"
            size="mini"
            show-jumper
            simple-only-jumper
            @change="loadCaseList"
            @page-size-change="loadCaseList"
          />
        </a-spin>
      </div>
      <!-- 右侧 -->
      <a-spin :loading="caseDetailLoading" class="relative flex h-full flex-1 flex-col overflow-hidden">
        <div class="flex px-[16px] pt-[16px]">
          <div class="mr-[24px] flex flex-1 items-center overflow-hidden">
            <MsStatusTag :status="caseDetail.lastExecuteResult || 'PREPARED'" />
            <div class="ml-[8px] mr-[2px] cursor-pointer font-medium text-[rgb(var(--primary-5))]" @click="goCaseDetail"
              >[{{ caseDetail.num }}]</div
            >
            <div class="flex-1 overflow-hidden">
              <a-tooltip :content="caseDetail.name">
                <div class="one-line-text max-w-[100%] font-medium">
                  {{ caseDetail.name }}
                </div>
              </a-tooltip>
            </div>
          </div>
          <a-button
            v-if="canEdit && hasAnyPermission(['FUNCTIONAL_CASE:READ+UPDATE'])"
            type="outline"
            @click="editCaseVisible = true"
            >{{ t('common.edit') }}</a-button
          >
        </div>
        <MsTab
          v-model:active-key="activeTab"
          :content-tab-list="contentTabList"
          no-content
          :get-text-func="getTotal"
          class="relative mx-[16px] border-b"
        />
        <div :class="[' flex-1', activeTab !== 'detail' ? 'tab-content' : 'overflow-hidden']">
          <MsDescription v-if="activeTab === 'baseInfo'" :descriptions="descriptions" :column="2" one-line-value>
            <template #value="{ item }">
              <template v-if="item.key === 'reviewStatus'">
                <MsIcon
                  :type="statusIconMap[item.value as keyof typeof statusIconMap]?.icon || ''"
                  class="mr-1"
                  :class="[statusIconMap[item.value as keyof typeof statusIconMap].color]"
                ></MsIcon>
                <span>{{ statusIconMap[item.value as keyof typeof statusIconMap]?.statusText || '' }} </span>
              </template>
            </template>
          </MsDescription>
          <div v-else-if="activeTab === 'detail'" class="align-content-start flex h-full flex-col">
            <CaseTabDetail ref="caseTabDetailRef" is-test-plan :form="caseDetail" :is-disabled-test-plan="!canEdit" />
            <!-- 开始执行 -->
            <div
              v-if="canEdit && hasAnyPermission(['PROJECT_TEST_PLAN:READ+EXECUTE'])"
              class="z-[101] px-[16px] py-[8px] shadow-[0_-1px_4px_rgba(2,2,2,0.1)]"
            >
              <div class="mb-[12px] flex items-center justify-between">
                <div class="font-medium text-[var(--color-text-1)]">
                  {{ t('testPlan.featureCase.startExecution') }}
                </div>
                <div class="flex items-center">
                  <a-switch v-model:model-value="autoNext" size="small" />
                  <div class="mx-[8px]">{{ t('caseManagement.caseReview.autoNext') }}</div>
                  <a-tooltip position="top">
                    <template #content>
                      <div>{{ t('testPlan.featureCase.autoNextTip1') }}</div>
                      <div>{{ t('testPlan.featureCase.autoNextTip2') }}</div>
                    </template>
                    <icon-question-circle
                      class="text-[var(--color-text-brand)] hover:text-[rgb(var(--primary-4))]"
                      size="16"
                    />
                  </a-tooltip>
                  <MsTag type="danger" theme="light" size="medium" class="ml-4">
                    <MsIcon type="icon-icon_defect" class="!text-[14px] text-[rgb(var(--danger-6))]" size="16" />
                    <span class="ml-1 text-[rgb(var(--danger-6))]"> {{ t('testPlan.featureCase.bug') }}</span>
                    <span class="ml-1 text-[rgb(var(--danger-6))]">{{ caseDetail.bugListCount }}</span>
                  </MsTag>
                  <a-dropdown @select="handleSelect">
                    <a-button v-if="hasAnyPermission(['PROJECT_BUG:READ'])" type="outline" size="small" class="ml-1">
                      <template #icon> <icon-plus class="text-[12px]" /> </template>
                    </a-button>
                    <template #content>
                      <a-doption
                        v-permission="['PROJECT_BUG:READ+ADD']"
                        :disabled="!hasAnyPermission(['PROJECT_BUG:READ+ADD'])"
                        value="new"
                        >{{ t('common.newCreate') }}</a-doption
                      >
                      <a-doption
                        v-if="createdBugCount > 0 && hasAnyPermission(['PROJECT_BUG:READ'])"
                        :disabled="!hasAnyPermission(['PROJECT_BUG:READ'])"
                        value="link"
                        >{{ t('common.associated') }}</a-doption
                      >
                      <a-popover v-else title="" position="left">
                        <a-doption
                          v-if="createdBugCount < 1 && hasAnyPermission(['PROJECT_BUG:READ'])"
                          :disabled="!hasAnyPermission(['PROJECT_BUG:READ'])"
                          value="link"
                          >{{ t('common.associated') }}</a-doption
                        >
                        <template #content>
                          <div class="flex items-center text-[14px]">
                            <span class="text-[var(--color-text-4)]">{{
                              t('testPlan.featureCase.noBugDataTooltip')
                            }}</span>
                            <MsButton
                              :disabled="!hasAnyPermission(['PROJECT_BUG:READ+ADD'])"
                              type="text"
                              @click="handleSelect('new')"
                            >
                              {{ t('testPlan.featureCase.noBugDataNewBug') }}
                            </MsButton>
                          </div>
                        </template>
                      </a-popover>
                    </template>
                  </a-dropdown>
                </div>
              </div>
              <ExecuteSubmit
                :id="activeId"
                :case-id="activeCaseId"
                :test-plan-id="route.query.id as string"
                :step-execution-result="stepExecutionResult"
                @done="executeDone"
              />
            </div>
          </div>
          <BugList
            v-if="activeTab === 'defectList'"
            ref="bugRef"
            :case-id="activeCaseId"
            :test-plan-case-id="activeId"
            :can-edit="canEdit"
            @link="linkDefect"
            @new="addBug"
            @update-count="loadCaseDetail()"
          />
          <ExecutionHistory
            v-if="activeTab === 'executionHistory'"
            :execute-list="executeHistoryList"
            :loading="executeLoading"
          />
        </div>
      </a-spin>
    </div>
  </MsCard>
  <EditCaseDetailDrawer v-model:visible="editCaseVisible" :case-id="activeCaseId" @load-case="loadCase" />
  <LinkDefectDrawer
    v-model:visible="showLinkDrawer"
    :case-id="activeCaseId"
    :drawer-loading="drawerLoading"
    :show-selector-all="false"
    @save="associateSuccessHandler"
  />
  <AddDefectDrawer
    v-model:visible="showDrawer"
    :case-id="activeCaseId"
    :extra-params="{
        testPlanCaseId: activeId,
        caseId: activeCaseId,
        testPlanId:route.query.id as string,
      }"
    @success="loadBugListAndCaseDetail"
  />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { Message } from '@arco-design/web-vue';
  import dayjs from 'dayjs';

  import MsButton from '@/components/pure/ms-button/index.vue';
  import MsCard from '@/components/pure/ms-card/index.vue';
  import MsDescription, { Description } from '@/components/pure/ms-description/index.vue';
  import MsEmpty from '@/components/pure/ms-empty/index.vue';
  import MsPagination from '@/components/pure/ms-pagination/index';
  import MsTab from '@/components/pure/ms-tab/index.vue';
  import MsTag from '@/components/pure/ms-tag/ms-tag.vue';
  import ExecuteResult from '@/components/business/ms-case-associate/executeResult.vue';
  import MsStatusTag from '@/components/business/ms-status-tag/index.vue';
  import BugList from './bug/index.vue';
  import ExecuteSubmit from './executeSubmit.vue';
  import AddDefectDrawer from '@/views/case-management/caseManagementFeature/components/tabContent/tabBug/addDefectDrawer.vue';
  import LinkDefectDrawer from '@/views/case-management/caseManagementFeature/components/tabContent/tabBug/linkDefectDrawer.vue';
  import CaseTabDetail from '@/views/case-management/caseManagementFeature/components/tabContent/tabDetail.vue';
  import EditCaseDetailDrawer from '@/views/case-management/caseReview/components/editCaseDetailDrawer.vue';
  import ExecutionHistory from '@/views/test-plan/testPlan/detail/featureCase/detail/executionHistory/index.vue';

  import { getBugList } from '@/api/modules/bug-management';
  import {
    associateBugToPlan,
    executeHistory,
    getCaseDetail,
    getPlanDetailFeatureCaseList,
    getTestPlanDetail,
  } from '@/api/modules/test-plan/testPlan';
  import { testPlanDefaultDetail } from '@/config/testPlan';
  import { useI18n } from '@/hooks/useI18n';
  import useOpenNewPage from '@/hooks/useOpenNewPage';
  import useAppStore from '@/store/modules/app';
  import { hasAnyPermission } from '@/utils/permission';

  import type { TableQueryParams } from '@/models/common';
  import type { ExecuteHistoryItem, PlanDetailFeatureCaseItem, TestPlanDetail } from '@/models/testPlan/testPlan';
  import { LastExecuteResults } from '@/enums/caseEnum';
  import { CaseManagementRouteEnum } from '@/enums/routeEnum';

  import {
    executionResultMap,
    getCustomField,
    statusIconMap,
  } from '@/views/case-management/caseManagementFeature/components/utils';

  const { t } = useI18n();
  const route = useRoute();
  const appStore = useAppStore();
  const { openNewPage } = useOpenNewPage();

  const planDetail = ref<TestPlanDetail>({
    ...testPlanDefaultDetail,
  });
  async function getPlanDetail() {
    try {
      planDetail.value = await getTestPlanDetail(route.query.id as string);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const activeCaseId = ref(route.query.caseId as string);
  const activeId = ref(route.query.testPlanCaseId as string);
  const canEdit = ref(route.query.canEdit === 'true');
  const keyword = ref('');
  const lastExecResult = ref('');
  const executeResultOptions = computed(() => {
    return [
      { label: t('common.all'), value: '' },
      ...Object.keys(executionResultMap).map((key) => {
        return {
          value: key,
          label: executionResultMap[key].statusText,
        };
      }),
    ];
  });
  const caseList = ref<PlanDetailFeatureCaseItem[]>([]);
  const pageNation = ref({
    total: 0,
    pageSize: 10,
    current: 1,
  });
  const otherListQueryParams = ref<Record<string, any>>({});
  const caseListLoading = ref(false);
  // 加载用例列表
  async function loadCaseList() {
    try {
      caseListLoading.value = true;
      const res = await getPlanDetailFeatureCaseList({
        projectId: appStore.currentProjectId,
        testPlanId: route.query.id as string,
        keyword: keyword.value,
        current: pageNation.value.current || 1,
        pageSize: pageNation.value.pageSize,
        filter: lastExecResult.value
          ? {
              lastExecResult: [lastExecResult.value],
            }
          : undefined,
        ...otherListQueryParams.value,
      });
      caseList.value = res.list;
      pageNation.value.total = res.total;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      caseListLoading.value = false;
    }
  }

  function goCaseDetail() {
    openNewPage(CaseManagementRouteEnum.CASE_MANAGEMENT_CASE, {
      id: activeCaseId.value,
    });
  }

  const caseDetail = ref<any>({});
  const caseDetailLoading = ref(false);
  const activeTab = ref('detail');
  const editCaseVisible = ref(false);
  const contentTabList = ref([
    {
      value: 'baseInfo',
      label: t('common.baseInfo'),
    },
    {
      value: 'detail',
      label: t('common.detail'),
    },
    {
      value: 'defectList',
      label: t('caseManagement.featureCase.defectList'),
    },
    {
      value: 'executionHistory',
      label: t('testPlan.featureCase.executionHistory'),
    },
  ]);
  const descriptions = ref<Description[]>([]);

  // 获取用例详情
  async function loadCaseDetail() {
    try {
      caseDetailLoading.value = true;
      const res = await getCaseDetail(activeId.value);
      caseDetail.value = res;
      descriptions.value = [
        {
          label: t('common.belongModule'),
          value: res.moduleName || t('common.root'),
        },
        {
          label: t('common.tag'),
          value: res.tags,
          isTag: true,
        },
        {
          label: t('caseManagement.featureCase.reviewResult'),
          value: res.reviewStatus,
          key: 'reviewStatus',
        },
        // 解析用例模板的自定义字段
        ...res.customFields.map((e: Record<string, any>) => {
          try {
            return {
              label: e.fieldName,
              value: getCustomField(e),
            };
          } catch (error) {
            return {
              label: e.fieldName,
              value: e.defaultValue,
            };
          }
        }),
        {
          label: t('common.creator'),
          value: res.createUserName,
        },
        {
          label: t('common.createTime'),
          value: dayjs(res.createTime).format('YYYY-MM-DD HH:mm:ss'),
        },
      ];
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      caseDetailLoading.value = false;
    }
  }

  function changeActiveCase(item: PlanDetailFeatureCaseItem) {
    if (activeId.value !== item.id) {
      activeCaseId.value = item.caseId;
      activeId.value = item.id;
      activeTab.value = 'detail';
    }
  }

  async function loadCase() {
    await loadCaseList();
    await loadCaseDetail();
  }

  const caseTabDetailRef = ref<InstanceType<typeof CaseTabDetail>>();
  const stepExecutionResult = computed(() => {
    const stepData = caseTabDetailRef.value?.stepData;
    return stepData?.map((item, index) => {
      return {
        id: item.id,
        num: index,
        desc: item.step,
        result: item.expected,
        actualResult: item.actualResult,
        executeResult: item.executeResult,
      };
    });
  });
  const autoNext = ref(true);
  async function executeDone() {
    if (autoNext.value) {
      // 自动下一个，更改激活的 id会刷新详情
      const index = caseList.value.findIndex((e) => e.id === activeId.value);
      if (index < caseList.value.length - 1) {
        await loadCaseList();
        activeCaseId.value = caseList.value[index + 1].caseId;
        activeId.value = caseList.value[index + 1].id;
      } else if (pageNation.value.current * pageNation.value.pageSize < pageNation.value.total) {
        // 当前页不是最后一页，则加载下一页并激活第一个用例
        pageNation.value.current += 1;
        await loadCaseList();
        activeCaseId.value = caseList.value[0].caseId;
        activeId.value = caseList.value[0].id;
      } else {
        // 当前是最后一个，刷新数据
        loadCaseDetail();
        loadCaseList();
      }
    } else {
      // 不自动下一个才请求详情
      loadCase();
    }
  }

  const showLinkDrawer = ref<boolean>(false);
  const drawerLoading = ref<boolean>(false);

  const showDrawer = ref<boolean>(false);

  function addBug() {
    showDrawer.value = true;
  }

  function linkDefect() {
    showLinkDrawer.value = true;
  }

  function handleSelect(value: string | number | Record<string, any> | undefined) {
    switch (value) {
      case 'new':
        addBug();
        break;
      default:
        linkDefect();
        break;
    }
  }
  const bugRef = ref();

  function addSuccess() {
    if (activeTab.value === 'defectList') {
      bugRef.value?.initData();
    }
  }

  function getTotal(key: string) {
    const { bugListCount } = caseDetail.value;
    switch (key) {
      case 'defectList':
        return bugListCount > 99 ? `99+` : `${bugListCount}`;
      default:
        return '';
    }
  }

  function loadBugListAndCaseDetail() {
    addSuccess();
    loadCaseDetail();
  }

  async function associateSuccessHandler(params: TableQueryParams) {
    try {
      drawerLoading.value = true;
      await associateBugToPlan({
        ...params,
        caseId: activeCaseId.value,
        testPlanId: route.query.id as string,
        testPlanCaseId: activeId.value,
      });
      Message.success(t('caseManagement.featureCase.associatedSuccess'));
      showLinkDrawer.value = false;
      loadBugListAndCaseDetail();
    } catch (error) {
      console.log(error);
    } finally {
      drawerLoading.value = false;
    }
  }

  const createdBugCount = ref<number>(0);

  async function initBugList() {
    if (!hasAnyPermission(['PROJECT_BUG:READ'])) {
      return;
    }
    const res = await getBugList({
      current: 1,
      pageSize: 10,
      sort: {},
      filter: {},
      keyword: '',
      combine: {},
      searchMode: 'AND',
      projectId: appStore.currentProjectId,
    });
    createdBugCount.value = res.total;
  }

  onBeforeMount(async () => {
    const lastPageParams = window.history.state.params ? JSON.parse(window.history.state.params) : null; // 获取上个页面带过来的表格查询参数
    if (lastPageParams) {
      const { total, pageSize, current, keyword: _keyword, sort, moduleIds } = lastPageParams;
      pageNation.value = {
        total: total || 0,
        pageSize,
        current,
      };
      keyword.value = _keyword;
      otherListQueryParams.value = {
        sort,
        moduleIds,
      };
    }
    getPlanDetail();
    initBugList();
    await loadCase();
  });

  const executeLoading = ref<boolean>(false);
  const executeHistoryList = ref<ExecuteHistoryItem[]>([]);
  async function initExecuteHistory() {
    executeLoading.value = true;
    try {
      executeHistoryList.value = await executeHistory({
        caseId: activeCaseId.value,
        id: activeId.value,
        testPlanId: route.query.id as string,
      });
    } catch (error) {
      console.log(error);
    } finally {
      executeLoading.value = false;
    }
  }
  watch(
    () => activeId.value,
    () => {
      loadCaseDetail();
      initBugList();
    }
  );

  watch(
    () => activeTab.value,
    (val) => {
      if (val === 'executionHistory') {
        initExecuteHistory();
      }
    }
  );
</script>

<style lang="less" scoped>
  .case-list {
    @apply flex flex-col  overflow-y-auto;

    margin-bottom: 16px;
    height: calc(100% - 40px);
    gap: 8px;
    .ms-scroll-bar();
    .case-item {
      @apply cursor-pointer;

      padding: 8px;
      border: 1px solid var(--color-text-n8);
      border-radius: var(--border-radius-small);
      &:hover {
        border: 1px solid rgb(var(--primary-4));
      }
    }
    .case-item--active {
      border: 1px solid rgb(var(--primary-5));
      background-color: var(--color-text-n9);
    }
  }
  .tab-content {
    @apply overflow-y-auto;

    padding: 16px;
    .ms-scroll-bar();
  }
  :deep(.caseDetailWrapper) {
    @apply flex-1 overflow-y-auto;

    padding: 16px;
    .ms-scroll-bar();
  }
</style>
