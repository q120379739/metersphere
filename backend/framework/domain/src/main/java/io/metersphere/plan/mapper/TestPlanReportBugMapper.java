package io.metersphere.plan.mapper;

import io.metersphere.plan.domain.TestPlanReportBug;
import io.metersphere.plan.domain.TestPlanReportBugExample;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TestPlanReportBugMapper {
    long countByExample(TestPlanReportBugExample example);

    int deleteByExample(TestPlanReportBugExample example);

    int insert(TestPlanReportBug record);

    int insertSelective(TestPlanReportBug record);

    List<TestPlanReportBug> selectByExample(TestPlanReportBugExample example);

    int updateByExampleSelective(@Param("record") TestPlanReportBug record, @Param("example") TestPlanReportBugExample example);

    int updateByExample(@Param("record") TestPlanReportBug record, @Param("example") TestPlanReportBugExample example);

    int batchInsert(@Param("list") List<TestPlanReportBug> list);

    int batchInsertSelective(@Param("list") List<TestPlanReportBug> list, @Param("selective") TestPlanReportBug.Column ... selective);
}