package io.metersphere.functional.controller;

import io.metersphere.functional.domain.FunctionalCaseComment;
import io.metersphere.functional.request.FunctionalCaseCommentRequest;
import io.metersphere.functional.service.FunctionalCaseCommentService;
import io.metersphere.sdk.constants.PermissionConstants;
import io.metersphere.system.utils.SessionUtils;
import io.metersphere.validation.groups.Created;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用例管理-功能用例-用例评论")
@RestController
@RequestMapping("/functional/case/comment")
public class FunctionalCaseCommentController {

    @Resource
    private FunctionalCaseCommentService functionalCaseCommentService;

    @PostMapping("/save")
    @Operation(summary = "用例管理-功能用例-用例评论-创建评论")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_COMMENT_READ_ADD)
    public FunctionalCaseComment saveComment(@Validated({Created.class}) @RequestBody FunctionalCaseCommentRequest functionalCaseCommentRequest) {
        return functionalCaseCommentService.saveComment(functionalCaseCommentRequest, SessionUtils.getUserId());
    }

    @GetMapping("/delete/{commentId}")
    @Operation(summary = "用例管理-功能用例-用例评论-删除评论")
    @RequiresPermissions(PermissionConstants.FUNCTIONAL_CASE_COMMENT_READ_DELETE)
    public void deleteComment(@PathVariable String commentId) {
        functionalCaseCommentService.deleteComment(commentId);
    }
}
