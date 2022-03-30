/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.crikkou.liferay.todo.list.service.impl;

import com.crikkou.liferay.todo.list.exception.TaskDueDateException;
import com.crikkou.liferay.todo.list.exception.TaskTitleException;
import com.crikkou.liferay.todo.list.model.Task;
import com.crikkou.liferay.todo.list.service.base.TaskLocalServiceBaseImpl;

import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.WorkflowInstanceLinkLocalService;
import com.liferay.portal.kernel.service.WorkflowInstanceLinkLocalServiceUtil;
import com.liferay.portal.kernel.service.permission.ModelPermissions;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowHandlerRegistryUtil;

import java.util.Date;

import org.osgi.service.component.annotations.Component;

/**
 * The implementation of the task local service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are
 * added, rerun ServiceBuilder to copy their definitions into the
 * <code>com.crikkou.liferay.todo.list.service.TaskLocalService</code>
 * interface.
 *
 * <p>
 * This is a local service. Methods of this service will not have security
 * checks based on the propagated JAAS credentials because this service can only
 * be accessed from within the same VM.
 * </p>
 *
 * @author Konstantinos Rikkou
 * @see TaskLocalServiceBaseImpl
 */
@Component(
	property = "model.class.name=com.crikkou.liferay.todo.list.model.Task",
	service = AopService.class
)
public class TaskLocalServiceImpl extends TaskLocalServiceBaseImpl {

	/**
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never reference this class directly. Use
	 * <code>com.crikkou.liferay.todo.list.service.TaskLocalService</code> via
	 * injection or a <code>org.osgi.util.tracker.ServiceTracker</code> or use
	 * <code>com.crikkou.liferay.todo.list.service.TaskLocalServiceUtil</code>.
	 */
	@Override
	public Task addTask(
			long userId, String title, String description, boolean completed,
			Date dueDate, ServiceContext serviceContext)
		throws PortalException {

		// Task

		User user = userLocalService.getUser(userId);
		long groupId = serviceContext.getScopeGroupId();

		validate(title, dueDate);

		long taskId = counterLocalService.increment();

		// TODO: friendlyURL validation

		Task task = taskPersistence.create(taskId);

		task.setUuid(serviceContext.getUuid());
		task.setGroupId(groupId);
		task.setCompanyId(user.getCompanyId());
		task.setUserId(user.getUserId());
		task.setUserName(user.getFullName());
		task.setTitle(title);
		task.setDescription(description);
		task.setCompleted(completed);
		task.setDueDate(dueDate);

		task = taskPersistence.update(task);

		// Resources

		if (serviceContext.isAddGroupPermissions() ||
			serviceContext.isAddGuestPermissions()) {

			addTaskResources(
				task, serviceContext.isAddGroupPermissions(),
				serviceContext.isAddGuestPermissions());
		}
		else {
			addTaskResources(task, serviceContext.getModelPermissions());
		}

		// TODO: Asset
		
		updateAsset(
				userId, task, serviceContext.getAssetCategoryIds(),
				serviceContext.getAssetTagNames());

		// TODO: Workflow
		
		WorkflowHandlerRegistryUtil.startWorkflowInstance(
				task.getCompanyId(), task.getGroupId(), userId,
				Task.class.getName(), task.getTaskId(), task, serviceContext);

		return task;
	}

	@Override
	public void addTaskResources(
			long taskId, boolean addGroupPermissions,
			boolean addGuestPermissions)
		throws PortalException {

		Task task = taskPersistence.findByPrimaryKey(taskId);

		addTaskResources(task, addGroupPermissions, addGuestPermissions);
	}

	@Override
	public void addTaskResources(long taskId, ModelPermissions modelPermissions)
		throws PortalException {

		Task task = taskPersistence.findByPrimaryKey(taskId);

		addTaskResources(task, modelPermissions);
	}

	@Override
	public void addTaskResources(
			Task task, boolean addGroupPermissions, boolean addGuestPermissions)
		throws PortalException {

		resourceLocalService.addResources(
			task.getCompanyId(), task.getGroupId(), task.getUserId(),
			Task.class.getName(), task.getTaskId(), false, addGroupPermissions,
			addGuestPermissions);
	}

	@Override
	public void addTaskResources(Task task, ModelPermissions modelPermissions)
		throws PortalException {

		resourceLocalService.addModelResources(
			task.getCompanyId(), task.getGroupId(), task.getUserId(),
			Task.class.getName(), task.getTaskId(), modelPermissions);
	}

	@Override
	public Task deleteTask(Task task) throws PortalException {

		// Task

		taskPersistence.remove(task);

		// Resources

		resourceLocalService.deleteResource(
			task.getCompanyId(), Task.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL, task.getTaskId());

		// TODO: Subscriptions

		// TODO: Asset
		
		assetEntryLocalService.deleteEntry(
				Task.class.getName(), task.getTaskId());

		// TODO: Expando

		// TODO: Trash

		// TODO: Workflow
		
		WorkflowInstanceLinkLocalServiceUtil.deleteWorkflowInstanceLinks(
				task.getCompanyId(), task.getGroupId(), Task.class.getName(),
				task.getTaskId());

		return task;
	}

	@Override
	public Task getTask(long taskId) throws PortalException {
		return taskPersistence.findByPrimaryKey(taskId);
	}

	public Task getTask(long groupId, String title) throws PortalException {
		return taskPersistence.fetchByG_T_First(groupId, title, null);
	}

	@Indexable(type = IndexableType.REINDEX)
	@Override
	public Task updateTask(
			long userId, long taskId, String title, String description,
			boolean completed, Date dueDate, ServiceContext serviceContext)
		throws PortalException {

		// Task

		Task task = taskPersistence.findByPrimaryKey(taskId);

		int status = task.getStatus();

		if (!task.isPending() && !task.isDraft()) {
			status = WorkflowConstants.STATUS_DRAFT;
		}

		validate(title, dueDate);

		task.setTitle(title);
		task.setDescription(description);
		task.setCompleted(completed);
		task.setDueDate(dueDate);

		task.setStatus(status);

		task.setExpandoBridgeAttributes(serviceContext);

		// TODO: Asset
		
		updateAsset(
				serviceContext.getUserId(), task,
				serviceContext.getAssetCategoryIds(),
				serviceContext.getAssetTagNames());

		task = taskPersistence.update(task);

		// TODO: Workflow

		return task;
	}

	public Task updateStatus(long userId, long taskId, int status) throws PortalException,
		       SystemException {

		    User user = userLocalService.getUser(userId);
		    Task task = getTask(taskId);

		    task.setStatus(status);
		    task.setStatusByUserId(userId);
		    task.setStatusByUserName(user.getFullName());
		    task.setStatusDate(new Date());

		    taskPersistence.update(task);

		    if (status == WorkflowConstants.STATUS_APPROVED) {
		       assetEntryLocalService.updateVisible(Task.class.getName(), taskId, true);
		    } else {
		       assetEntryLocalService.updateVisible(Task.class.getName(), taskId, false);
		    }

		    return task;
		}
	
	@Override
	public void updateAsset(
			long userId, Task task, long[] assetCategoryIds,
			String[] assetTagNames)
		throws PortalException {

		boolean visible = false;

		if (task.isApproved()) {
			visible = true;
		}

		assetEntryLocalService.updateEntry(
			userId, task.getGroupId(), task.getCreateDate(),
			task.getModifiedDate(), Task.class.getName(), task.getTaskId(),
			task.getUuid(), 0, assetCategoryIds, assetTagNames, true, visible,
			null, null, null, null, ContentTypes.TEXT_HTML, task.getTitle(),
			null, null, null, null, 0, 0, null);
	}
	
	@Override
	public void updateTaskResources(
			Task entry, ModelPermissions modelPermissions)
		throws PortalException {

		resourceLocalService.updateResources(
			entry.getCompanyId(), entry.getGroupId(), Task.class.getName(),
			entry.getTaskId(), modelPermissions);
	}

	@Override
	public void updateTaskResources(
			Task entry, String[] groupPermissions, String[] guestPermissions)
		throws PortalException {

		resourceLocalService.updateResources(
			entry.getCompanyId(), entry.getGroupId(), Task.class.getName(),
			entry.getTaskId(), groupPermissions, guestPermissions);
	}

	protected void validate(String title, Date dueDate) throws PortalException {
		if (Validator.isNull(title)) {
			throw new TaskTitleException();
		}

		if (Validator.isNull(dueDate)) {
			throw new TaskDueDateException();
		}
	}

}