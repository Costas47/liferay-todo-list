package com.crikkou.liferay.todo.list.internal.workflow;

import com.crikkou.liferay.todo.list.model.Task;
import com.crikkou.liferay.todo.list.service.TaskLocalService;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.security.permission.ResourceActionsUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.workflow.BaseWorkflowHandler;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowHandler;

import java.io.Serializable;

import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Konstantinos Rikkou
 */
@Component(
	property = "model.class.name=com.crikkou.liferay.todo.list.model.Task",
	service = WorkflowHandler.class
)
public class TaskWorkflowHandler extends BaseWorkflowHandler<Task> {

	@Override
	public String getClassName() {
		return Task.class.getName();
	}

	@Override
	public String getType(Locale locale) {
		return ResourceActionsUtil.getModelResource(locale, getClassName());
	}

	@Override
	public Task updateStatus(
			int status, Map<String, Serializable> workflowContext)
		throws PortalException {

		long userId = GetterUtil.getLong(
			(String)workflowContext.get(WorkflowConstants.CONTEXT_USER_ID));

		long classPK = GetterUtil.getLong(
			(String)workflowContext.get(
				WorkflowConstants.CONTEXT_ENTRY_CLASS_PK));

		Task task = _taskLocalService.getTask(classPK);

		return _taskLocalService.updateStatus(userId, task.getTaskId(), status);
	}

	@Reference
	private TaskLocalService _taskLocalService;

}