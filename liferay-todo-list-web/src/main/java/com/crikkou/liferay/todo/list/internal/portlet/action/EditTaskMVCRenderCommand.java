package com.crikkou.liferay.todo.list.internal.portlet.action;

import com.crikkou.liferay.todo.list.constants.ToDoListPortletKeys;
import com.crikkou.liferay.todo.list.exception.NoSuchTaskException;
import com.crikkou.liferay.todo.list.internal.util.WebKeys;
import com.crikkou.liferay.todo.list.model.Task;

import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.servlet.SessionErrors;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;

/**
 * @author Konstantinos Rikkou
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + ToDoListPortletKeys.TODO_LIST,
		"mvc.command.name=/edit_task"
	},
	service = MVCRenderCommand.class
)
public class EditTaskMVCRenderCommand implements MVCRenderCommand {

	@Override
	public String render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws PortletException {

		try {
			Task task = ActionUtil.getTask(renderRequest);

			renderRequest.setAttribute(WebKeys.TASK, task);
		}
		catch (NoSuchTaskException | PrincipalException e) {
			SessionErrors.add(renderRequest, e.getClass());

			return "/error.jsp";
		}
		catch (RuntimeException re) {
			throw re;
		}
		catch (Exception e) {
			throw new PortletException(e);
		}

		return "/edit_task.jsp";
	}

}