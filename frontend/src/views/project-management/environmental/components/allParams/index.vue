<template>
  <div class="mb-[8px] flex items-center justify-between">
    <a-input-search
      v-model="searchValue"
      :placeholder="t('project.environmental.searchParamsHolder')"
      allow-clear
      class="w-[240px]"
      @search="handleSearch"
      @press-enter="handleSearch"
      @clear="handleSearch"
    >
    </a-input-search>
  </div>
  <paramsTable
    :params="innerParams"
    :table-key="props.tableKey"
    :columns="columns"
    :draggable="true"
    show-setting
    :selectable="true"
    :default-param-item="defaultParamItem"
    @change="handleParamTableChange"
    @batch-add="batchAddKeyValVisible = true"
  />
  <batchAddKeyVal
    v-model:visible="batchAddKeyValVisible"
    :add-type-text="t('project.environmental.env.constantBatchAddTip')"
    :params="innerParams"
    no-param-type
    @apply="handleBatchParamApply"
  />
</template>

<script setup lang="ts">
  import batchAddKeyVal from '@/views/api-test/components/batchAddKeyVal.vue';
  import paramsTable, { type ParamTableColumn } from '@/views/api-test/components/paramTable.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { TableKeyEnum } from '@/enums/tableEnum';

  defineOptions({ name: 'EnvManagementAllParams' });

  const props = withDefaults(
    defineProps<{
      tableKey: TableKeyEnum;
    }>(),
    {
      tableKey: TableKeyEnum.PROJECT_MANAGEMENT_ENV_ALL_PARAM,
    }
  );
  const emit = defineEmits<{
    (e: 'change'): void; //  数据发生变化
  }>();

  const searchValue = ref('');

  const { t } = useI18n();

  const innerParams = defineModel<any[]>('params', {
    required: true,
  });
  const backupParams = ref(innerParams.value);
  const firstSearch = ref(true);
  const batchAddKeyValVisible = ref(false);

  const defaultParamItem = {
    key: '',
    paramType: 'CONSTANT',
    value: '',
    description: '',
    tags: [],
    enable: true,
  };

  const columns: ParamTableColumn[] = [
    {
      title: 'project.environmental.paramName',
      dataIndex: 'key',
      slotName: 'key',
      showInTable: true,
      showDrag: true,
      columnSelectorDisabled: true,
    },
    {
      title: 'project.environmental.paramType',
      dataIndex: 'paramType',
      slotName: 'paramType',
      showInTable: true,
      showDrag: true,
      hasRequired: false,
      columnSelectorDisabled: true,
      options: [
        {
          label: t('common.constant'),
          value: 'CONSTANT',
        },
        {
          label: t('common.list'),
          value: 'LIST',
        },
        /* {
          label: t('common.json'),
          value: 'JSON',
        }, */
      ],
      // TODO 这个版本暂时不展示
      // titleSlotName: 'typeTitle',
      // typeTitleTooltip: t('project.environmental.paramTypeTooltip'),
    },
    {
      title: 'project.environmental.paramValue',
      dataIndex: 'value',
      slotName: 'value',
      showInTable: true,
      showDrag: true,
      columnSelectorDisabled: true,
    },
    {
      title: 'project.environmental.tag',
      dataIndex: 'tags',
      slotName: 'tag',
      width: 200,
      showInTable: true,
      showDrag: true,
    },
    {
      title: 'common.desc',
      dataIndex: 'description',
      slotName: 'description',
      showInTable: true,
      showDrag: true,
    },
    {
      title: '',
      slotName: 'operation',
      titleSlotName: 'batchAddTitle',
      dataIndex: 'operation',
      width: 100,
    },
  ];

  /**
   * 批量参数代码转换为参数表格数据
   */
  function handleBatchParamApply(resultArr: any[]) {
    resultArr.forEach((item) => {
      item.paramType = 'CONSTANT';
    });
    if (resultArr.length < innerParams.value.length) {
      innerParams.value.splice(0, innerParams.value.length - 1, ...resultArr);
    } else {
      innerParams.value = [...resultArr, innerParams.value[innerParams.value.length - 1]];
    }
    emit('change');
  }

  function handleParamTableChange(resultArr: any[], isInit?: boolean) {
    innerParams.value = [...resultArr];
    if (!isInit) {
      emit('change');
      firstSearch.value = true;
    }
  }

  function handleSearch() {
    if (firstSearch.value) {
      backupParams.value = [...innerParams.value];
      firstSearch.value = false;
    }
    if (!searchValue.value) {
      innerParams.value = [...backupParams.value];
    } else {
      const result = backupParams.value.filter(
        (item) => item.key?.includes(searchValue.value) || (item.tags || []).includes(searchValue.value)
      );
      innerParams.value = [...result];
    }
  }
</script>

<style lang="less" scoped></style>
