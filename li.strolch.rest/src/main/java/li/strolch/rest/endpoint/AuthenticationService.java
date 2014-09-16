/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package li.strolch.rest.endpoint;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import li.strolch.exception.StrolchException;
import li.strolch.rest.RestfulStrolchComponent;
import li.strolch.rest.StrolchSessionHandler;
import li.strolch.rest.helper.RestfulHelper;
import li.strolch.rest.model.Login;
import li.strolch.rest.model.LoginResult;
import li.strolch.rest.model.LogoutResult;
import li.strolch.runtime.privilege.PrivilegeHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.eitchnet.privilege.base.PrivilegeException;
import ch.eitchnet.privilege.model.Certificate;
import ch.eitchnet.privilege.model.PrivilegeContext;
import ch.eitchnet.utils.helper.StringHelper;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
@Path("strolch/authentication")
public class AuthenticationService {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(Login login, @Context HttpServletRequest request, @Context HttpHeaders headers) {

		LoginResult loginResult = new LoginResult();
		GenericEntity<LoginResult> entity = new GenericEntity<LoginResult>(loginResult, LoginResult.class) {
			//
		};

		try {

			StringBuilder sb = new StringBuilder();
			if (StringHelper.isEmpty(login.getUsername())) {
				sb.append("Username was not given. "); //$NON-NLS-1$
			}
			if (StringHelper.isEmpty(login.getPassword())) {
				sb.append("Password was not given."); //$NON-NLS-1$
			}

			if (sb.length() != 0) {
				loginResult.setMsg(MessageFormat.format("Could not log in due to: {0}", sb.toString())); //$NON-NLS-1$
				return Response.status(Status.UNAUTHORIZED).entity(loginResult).build();
			}

			StrolchSessionHandler sessionHandler = RestfulStrolchComponent.getInstance().getComponent(
					StrolchSessionHandler.class);
			String origin = request == null ? "test" : request.getRemoteAddr(); //$NON-NLS-1$
			Certificate certificate = sessionHandler.authenticate(origin, login.getUsername(), login.getPassword()
					.getBytes());

			Locale locale = RestfulHelper.getLocale(headers);
			certificate.setLocale(locale);

			PrivilegeHandler privilegeHandler = RestfulStrolchComponent.getInstance().getPrivilegeHandler();
			PrivilegeContext privilegeContext = privilegeHandler.getPrivilegeContext(certificate);
			loginResult.setSessionId(certificate.getAuthToken());
			loginResult.setUsername(certificate.getUsername());
			loginResult.setLocale(locale.toString());
			loginResult.setParameters(certificate.getPropertyMap());

			List<String> allowList = privilegeContext.getFlatAllowList();
			if (allowList.isEmpty())
				loginResult.setPrivileges(Arrays.asList("*")); //$NON-NLS-1$
			else
				loginResult.setPrivileges(allowList);

			return Response.ok().entity(entity).build();

		} catch (StrolchException e) {
			logger.error(e.getMessage(), e);
			loginResult.setMsg(MessageFormat.format("Could not log in due to: {0}", e.getMessage())); //$NON-NLS-1$
			return Response.status(Status.UNAUTHORIZED).entity(entity).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			loginResult.setMsg(MessageFormat.format("{0}: {1}", e.getClass().getName(), msg)); //$NON-NLS-1$
			return Response.serverError().entity(entity).build();
		}
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{authToken}")
	public Response logout(@PathParam("authToken") String authToken, @Context HttpServletRequest request) {

		LogoutResult logoutResult = new LogoutResult();

		GenericEntity<LogoutResult> entity = new GenericEntity<LogoutResult>(logoutResult, LogoutResult.class) {
			//
		};
		try {

			StrolchSessionHandler sessionHandlerHandler = RestfulStrolchComponent.getInstance().getComponent(
					StrolchSessionHandler.class);
			String origin = request == null ? "test" : request.getRemoteAddr(); //$NON-NLS-1$
			Certificate certificate = sessionHandlerHandler.validate(origin, authToken);
			sessionHandlerHandler.invalidateSession(origin, certificate);

			logoutResult.setMsg(MessageFormat.format("{0} has been logged out.", certificate.getUsername())); //$NON-NLS-1$
			return Response.ok().entity(entity).build();

		} catch (StrolchException | PrivilegeException e) {
			logger.error(e.getMessage(), e);
			logoutResult.setMsg(MessageFormat.format("Could not logout due to: {0}", e.getMessage())); //$NON-NLS-1$
			return Response.status(Status.UNAUTHORIZED).entity(entity).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			String msg = e.getMessage();
			logoutResult.setMsg(MessageFormat.format("{0}: {1}", e.getClass().getName(), msg)); //$NON-NLS-1$
			return Response.serverError().entity(entity).build();
		}
	}
}