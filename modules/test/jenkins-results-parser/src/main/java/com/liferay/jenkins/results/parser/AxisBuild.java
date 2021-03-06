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

package com.liferay.jenkins.results.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Peter Yoo
 */
public class AxisBuild extends BaseBuild {

	@Override
	public void findDownstreamBuilds() {
	}

	public String getAxisVariable() {
		return axisVariable;
	}

	@Override
	public String getBuildURL() {
		try {
			String jobURL = getJobURL();
			int buildNumber = getBuildNumber();

			if ((jobURL == null) || (buildNumber == -1)) {
				return null;
			}

			jobURL = JenkinsResultsParserUtil.decode(jobURL);

			return JenkinsResultsParserUtil.encode(
				jobURL + "/" + axisVariable + "/" + buildNumber + "/");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reinvoke() {
		throw new RuntimeException("Axis builds cannot be reinvoked");
	}

	protected AxisBuild(String url) throws Exception {
		this(url, null);
	}

	protected AxisBuild(String url, BatchBuild parentBuild) throws Exception {
		super(JenkinsResultsParserUtil.getLocalURL(url), parentBuild);
	}

	@Override
	protected void checkForReinvocation() {
	}

	@Override
	protected void setBuildURL(String buildURL) throws Exception {
		buildURL = JenkinsResultsParserUtil.decode(buildURL);

		Matcher matcher = _buildURLPattern.matcher(buildURL);

		if (!matcher.find()) {
			throw new IllegalArgumentException("Invalid build URL " + buildURL);
		}

		axisVariable = matcher.group("axisVariable");
		jobName = matcher.group("jobName");
		master = matcher.group("master");

		setBuildNumber(Integer.parseInt(matcher.group("buildNumber")));

		loadParametersFromBuildJSONObject();

		setStatus("running");
	}

	protected String axisVariable;

	private static final Pattern _buildURLPattern = Pattern.compile(
		"\\w+://(?<master>[^/]+)/+job/+(?<jobName>[^/]+)/" +
			"(?<axisVariable>AXIS_VARIABLE=[^,]+,[^/]+)/" +
				"(?<buildNumber>\\d+)/?");

}