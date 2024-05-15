package io.metersphere.plan.controller;

import io.metersphere.plan.dto.TestPlanShareInfo;
import io.metersphere.plan.dto.request.*;
import io.metersphere.plan.dto.response.TestPlanReportPageResponse;
import io.metersphere.sdk.constants.ShareInfoType;
import io.metersphere.sdk.util.JSON;
import io.metersphere.system.base.BaseTest;
import io.metersphere.system.controller.handler.ResultHolder;
import io.metersphere.system.utils.Pager;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureMockMvc
public class TestPlanReportControllerTests extends BaseTest {

	private static final String LIST_PLAN_REPORT = "/test-plan/report/page";
	private static final String RENAME_PLAN_REPORT = "/test-plan/report/rename";
	private static final String DELETE_PLAN_REPORT = "/test-plan/report/delete";
	private static final String BATCH_DELETE_PLAN_REPORT = "/test-plan/report/batch-delete";
	private static final String GEN_AND_SHARE = "/test-plan/report/share/gen";
	private static final String GET_SHARE_INFO = "/test-plan/report/share/get";
	private static final String GET_SHARE_TIME = "/test-plan/report/share/get-share-time";

	@Test
	@Order(1)
	@Sql(scripts = {"/dml/init_test_plan_report.sql"}, config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED))
	void tesPagePlanReportSuccess() throws Exception {
		TestPlanReportPageRequest request = new TestPlanReportPageRequest();
		request.setProjectId("100001100001");
		request.setType("ALL");
		request.setCurrent(1);
		request.setPageSize(10);
		request.setKeyword("1");
		MvcResult mvcResult = this.requestPostWithOkAndReturn(LIST_PLAN_REPORT, request);
		// 获取返回值
		String returnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
		ResultHolder resultHolder = JSON.parseObject(returnData, ResultHolder.class);
		// 返回请求正常
		Assertions.assertNotNull(resultHolder);
		Pager<?> pageData = JSON.parseObject(JSON.toJSONString(resultHolder.getData()), Pager.class);
		// 返回值不为空
		Assertions.assertNotNull(pageData);
		// 返回值的页码和当前页码相同
		Assertions.assertEquals(pageData.getCurrent(), request.getCurrent());
		// 返回的数据量不超过规定要返回的数据量相同
		Assertions.assertTrue(JSON.parseArray(JSON.toJSONString(pageData.getList())).size() <= request.getPageSize());
		// 返回值中取出第一条数据, 并判断是否包含关键字default
		TestPlanReportPageResponse report = JSON.parseArray(JSON.toJSONString(pageData.getList()), TestPlanReportPageResponse.class).get(0);
		Assertions.assertTrue(StringUtils.contains(report.getName(), request.getKeyword()));
		// 覆盖排序, 及数据为空
		request.setSort(Map.of("tpr.create_time", "asc"));
		request.setKeyword("oasis");
		this.requestPost(LIST_PLAN_REPORT, request);
	}

	@Test
	@Order(2)
	void testPagePlanReportError() throws Exception {
		// 必填参数有误
		TestPlanReportPageRequest request = new TestPlanReportPageRequest();
		request.setCurrent(1);
		request.setPageSize(10);
		this.requestPost(LIST_PLAN_REPORT, request, status().isBadRequest());
		// 页码有误
		request.setProjectId("100001100001");
		request.setType("ALL");
		request.setCurrent(0);
		request.setPageSize(10);
		this.requestPost(LIST_PLAN_REPORT, request, status().isBadRequest());
		// 页数有误
		request.setCurrent(1);
		request.setPageSize(1);
		this.requestPost(LIST_PLAN_REPORT, request, status().isBadRequest());
	}

	@Test
	@Order(3)
	void testRenamePlanReportSuccess() throws Exception {
		TestPlanReportEditRequest request = new TestPlanReportEditRequest();
		request.setId("test-plan-report-id-1");
		request.setName("oasis");
		request.setProjectId("100001100001");
		this.requestPostWithOk(RENAME_PLAN_REPORT, request);
	}

	@Test
	@Order(4)
	void testRenamePlanReportError() throws Exception {
		TestPlanReportEditRequest request = new TestPlanReportEditRequest();
		request.setId("test-plan-report-id-x");
		request.setName("oasis");
		request.setProjectId("100001100001");
		this.requestPost(RENAME_PLAN_REPORT, request, status().is5xxServerError());
	}

	@Test
	@Order(5)
	void testSharePlanReport() throws Exception {
		TestPlanReportShareRequest shareRequest = new TestPlanReportShareRequest();
		shareRequest.setReportId("test-plan-report-id-1");
		shareRequest.setProjectId("100001100001");
		shareRequest.setShareType(ShareInfoType.TEST_PLAN_SHARE_REPORT.name());
		this.requestPost(GEN_AND_SHARE, shareRequest);
		shareRequest.setLang(Locale.SIMPLIFIED_CHINESE.getLanguage());
		MvcResult mvcResult = this.requestPost(GEN_AND_SHARE, shareRequest).andReturn();
		String sortData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
		ResultHolder sortHolder = JSON.parseObject(sortData, ResultHolder.class);
		TestPlanShareInfo shareInfo = JSON.parseObject(JSON.toJSONString(sortHolder.getData()), TestPlanShareInfo.class);
		Assertions.assertNotNull(shareInfo);
		this.requestGet(GET_SHARE_INFO + "/" + shareInfo.getId());
	}

	@Test
	@Order(6)
	void testGetShareInfo() throws Exception {
		// 报告被删除
		this.requestGet(GET_SHARE_INFO + "/share-1");
		// 分享链接未找到
		this.requestGet(GET_SHARE_INFO + "/share-2", status().is5xxServerError());
	}

	@Test
	@Order(7)
	void testGetShareTime() throws Exception {
		this.requestGet(GET_SHARE_TIME + "/100001100001");
		this.requestGet(GET_SHARE_TIME + "/100001100002");
	}

	@Test
	@Order(8)
	void testDeletePlanReport() throws Exception {
		TestPlanReportDeleteRequest request = new TestPlanReportDeleteRequest();
		request.setId("test-plan-report-id-1");
		request.setProjectId("100001100001");
		this.requestPostWithOk(DELETE_PLAN_REPORT, request);
	}

	@Test
	@Order(9)
	void testBatchDeletePlanReport() throws Exception {
		TestPlanReportBatchRequest request = new TestPlanReportBatchRequest();
		request.setProjectId("100001100001");
		request.setType("ALL");
		// 勾选部分, 并删除
		request.setSelectAll(false);
		request.setSelectIds(List.of("test-plan-report-id-2"));
		this.requestPostWithOk(BATCH_DELETE_PLAN_REPORT, request);
		// 全选并排除所有, 为空
		request.setSelectAll(true);
		request.setExcludeIds(List.of("test-plan-report-id-3", "test-plan-report-id-4"));
		this.requestPostWithOk(BATCH_DELETE_PLAN_REPORT, request);
		// 全选不排除
		request.setExcludeIds(null);
		this.requestPostWithOk(BATCH_DELETE_PLAN_REPORT, request);
	}
}
