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

package com.liferay.productivitycenter.layout;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.io.unsync.UnsyncStringWriter;
import com.liferay.portal.kernel.servlet.BrowserSnifferUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.LayoutTypeController;
import com.liferay.portal.util.Portal;
import com.liferay.portal.util.WebKeys;
import com.liferay.taglib.servlet.PipingServletResponse;

import java.net.URL;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Adolfo Pérez
 */
@Component(
	immediate = true, property = { "layout.type=user_personal_panel" },
	service = LayoutTypeController.class
)
public class UserPersonalPanelLayoutController implements LayoutTypeController {

	@Override
	public String[] getConfigurationActionDelete() {
		return _NO_CONFIGURATION_ACTIONS;
	}

	@Override
	public String[] getConfigurationActionUpdate() {
		return _NO_CONFIGURATION_ACTIONS;
	}

	@Override
	public String getEditPage() {
		return _EDIT_PAGE;
	}

	@Override
	public String getURL() {
		return _URL;
	}

	@Override
	public boolean includeLayoutContent(
			HttpServletRequest request, HttpServletResponse response,
			Layout layout)
		throws Exception {

		String portletId = ParamUtil.getString(request, "p_p_id");

		String path = getViewPath(portletId, BrowserSnifferUtil.isWap(request));

		RequestDispatcher requestDispatcher = request.getRequestDispatcher(
			Portal.PATH_MODULE + StringPool.SLASH + _servletContextName + path);

		UnsyncStringWriter unsyncStringWriter = new UnsyncStringWriter();

		PipingServletResponse pipingServletResponse = new PipingServletResponse(
			response, unsyncStringWriter);

		String contentType = pipingServletResponse.getContentType();

		requestDispatcher.include(request, pipingServletResponse);

		if (contentType != null) {
			response.setContentType(contentType);
		}

		request.setAttribute(
			WebKeys.LAYOUT_CONTENT, unsyncStringWriter.getStringBundler());

		return false;
	}

	@Override
	public boolean isFirstPageable() {
		return true;
	}

	@Override
	public boolean isParentable() {
		return false;
	}

	@Override
	public boolean isSitemapable() {
		return _SITEMAPABLE;
	}

	@Override
	public boolean isURLFriendliable() {
		return _URL_FRIENDLIABLE;
	}

	@Override
	public boolean matches(
		HttpServletRequest request, String friendlyURL, Layout layout) {

		try {
			Map<Locale, String> friendlyURLMap = layout.getFriendlyURLMap();

			Collection<String> values = friendlyURLMap.values();

			return values.contains(friendlyURL);
		}
		catch (SystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_servletContextName = bundleContext.getBundle().getSymbolicName();

		_servletServiceRegistration = createJspServlet(bundleContext);

		_servletContextHelperServiceRegistration = createContext(
			bundleContext.getBundle());
	}

	protected ServiceRegistration<ServletContextHelper> createContext(
		Bundle bundle) {

		ServletContextHelper servletContextHelper =
			new ServletContextHelper(bundle) {

				@Override
				public URL getResource(String name) {
					return super.getResource("/META-INF/resources" + name);
				}

			};

		BundleContext bundleContext = bundle.getBundleContext();

		Dictionary<String, Object> properties = new HashMapDictionary<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
			_servletContextName);
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
			"/" + bundle.getSymbolicName());

		return bundleContext.registerService(
			ServletContextHelper.class, servletContextHelper, properties);
	}

	protected ServiceRegistration<Servlet> createJspServlet(
		BundleContext bundleContext) {

		Servlet servlet = null;

		try {
			Class<?> clazz = Class.forName(
				"com.liferay.portal.servlet.jsp.compiler.JspServlet");

			servlet = (Servlet)clazz.newInstance();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		Dictionary<String, Object> properties = new HashMapDictionary<>();

		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
			_servletContextName);
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "jsp");
		properties.put(
			HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "*.jsp");

		return bundleContext.registerService(
			Servlet.class, servlet, properties);
	}

	@Deactivate
	protected void deactivate() {
		_servletServiceRegistration.unregister();

		_servletContextHelperServiceRegistration.unregister();
	}

	protected String getViewPath(String portletId, boolean wap) {
		return _VIEW_PATH;
	}

	private static final String _EDIT_PAGE =
		"/layout/edit/user_personal_panel.jsp";

	private static final String[] _NO_CONFIGURATION_ACTIONS = new String[0];

	private static final boolean _SITEMAPABLE = GetterUtil.getBoolean(
		PropsUtil.get(PropsKeys.LAYOUT_SITEMAPABLE), true);

	private static final String _URL = GetterUtil.getString(
		PropsUtil.get(PropsKeys.LAYOUT_URL));

	private static final boolean _URL_FRIENDLIABLE = GetterUtil.getBoolean(
		PropsUtil.get(PropsKeys.LAYOUT_URL_FRIENDLIABLE), true);

	private static final String _VIEW_PATH =
		"/layout/view/user_personal_panel.jsp";

	private ServiceRegistration<ServletContextHelper>
		_servletContextHelperServiceRegistration;
	private String _servletContextName;
	private ServiceRegistration<Servlet> _servletServiceRegistration;

}