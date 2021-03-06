package com.crikkou.liferay.todo.list.internal.portlet.action;

import com.crikkou.liferay.todo.list.model.Task;
import com.crikkou.liferay.todo.list.service.TaskServiceUtil;

import com.liferay.portal.kernel.util.ParamUtil;

import javax.portlet.PortletRequest;

/**
 * @author Konstantinos Rikkou
 */
public class ActionUtil {

	public static Task getTask(PortletRequest portletRequest) throws Exception {
		long taskId = ParamUtil.getLong(portletRequest, "taskId");

		Task task = null;

		if (taskId > 0) {
			task = TaskServiceUtil.getTask(taskId);
		}

		// TODO: Add trashHelper support

		//		if ((task != null) && task.isInTrash()) {
		//			throw new NoSuchTaskException("{taskId=" + taskId + "}");
		//		}

		return task;
	}

}