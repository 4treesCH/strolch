/*
 * Copyright (c) 2010
 * 
 * Robert von Burg
 * eitch@eitchnet.ch
 * 
 * All rights reserved.
 * 
 */

package ch.eitchnet.privilege.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;

import ch.eitchnet.privilege.helper.ClassHelper;
import ch.eitchnet.privilege.helper.XmlConstants;
import ch.eitchnet.privilege.helper.XmlHelper;
import ch.eitchnet.privilege.i18n.PrivilegeException;
import ch.eitchnet.privilege.model.UserState;
import ch.eitchnet.privilege.model.internal.Privilege;
import ch.eitchnet.privilege.model.internal.Role;
import ch.eitchnet.privilege.model.internal.User;
import ch.eitchnet.privilege.policy.PrivilegePolicy;

/**
 * @author rvonburg
 * 
 */
public class XmlPersistenceHandler implements PersistenceHandler {

	private static final Logger logger = Logger.getLogger(XmlPersistenceHandler.class);

	private Map<String, User> userMap;
	private Map<String, Role> roleMap;
	private Map<String, Privilege> privilegeMap;
	private Map<String, Class<PrivilegePolicy>> policyMap;

	private long usersFileDate;
	private boolean userMapDirty;
	private long rolesFileDate;
	private boolean roleMapDirty;
	private long privilegesFileDate;
	private boolean privilegeMapDirty;

	private Map<String, String> parameterMap;

	private String basePath;

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#addOrReplacePrivilege(ch.eitchnet.privilege.model.internal.Privilege)
	 */
	@Override
	public void addOrReplacePrivilege(Privilege privilege) {
		this.privilegeMap.put(privilege.getName(), privilege);
		this.privilegeMapDirty = true;
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#removePrivilege(java.lang.String)
	 */
	@Override
	public Privilege removePrivilege(String privilegeName) {
		Privilege privilege = this.privilegeMap.remove(privilegeName);
		this.privilegeMapDirty = privilege != null;
		return privilege;
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#addOrReplaceRole(ch.eitchnet.privilege.model.internal.Role)
	 */
	@Override
	public void addOrReplaceRole(Role role) {
		this.roleMap.put(role.getName(), role);
		this.roleMapDirty = true;
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#removeRole(java.lang.String)
	 */
	@Override
	public Role removeRole(String roleName) {
		Role role = this.roleMap.remove(roleName);
		this.roleMapDirty = role != null;
		return role;
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#addOrReplaceUser(ch.eitchnet.privilege.model.internal.User)
	 */
	@Override
	public void addOrReplaceUser(User user) {
		this.userMap.put(user.getUsername(), user);
		this.userMapDirty = true;
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#removeUser(java.lang.String)
	 */
	@Override
	public User removeUser(String username) {
		User user = this.userMap.remove(username);
		this.userMapDirty = user != null;
		return user;
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#getPrivilege(java.lang.String)
	 */
	@Override
	public Privilege getPrivilege(String privilegeName) {
		return this.privilegeMap.get(privilegeName);
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#getRole(java.lang.String)
	 */
	@Override
	public Role getRole(String roleName) {
		return this.roleMap.get(roleName);
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#getUser(java.lang.String)
	 */
	@Override
	public User getUser(String username) {
		return this.userMap.get(username);
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#getPolicy(java.lang.String)
	 */
	@Override
	public PrivilegePolicy getPolicy(String policyName) {

		// get the policies class
		Class<PrivilegePolicy> policyClazz = this.policyMap.get(policyName);
		if (policyClazz == null) {
			return null;
		}

		// instantiate the policy
		PrivilegePolicy policy;
		try {
			policy = ClassHelper.instantiateClass(policyClazz);
		} catch (Exception e) {
			throw new PrivilegeException("The class for the policy with the name " + policyName + " does not exist!"
					+ policyName, e);
		}

		return policy;
	}

	/**
	 * @see ch.eitchnet.privilege.handler.PersistenceHandler#persist()
	 */
	@Override
	public boolean persist() {

		// USERS
		// get users file name
		String usersFileName = this.parameterMap.get(XmlConstants.XML_PARAM_USERS_FILE);
		if (usersFileName == null || usersFileName.isEmpty()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_USERS_FILE + " is invalid");
		}
		// get users file
		File usersFile = new File(this.basePath + "/" + usersFileName);
		boolean usersFileUnchanged = usersFile.exists() && usersFile.lastModified() == this.usersFileDate;
		if (!this.userMapDirty && usersFileUnchanged) {
			logger.warn("No users unpersisted and user file unchanged on file system");
		} else {
			logger.info("Persisting users...");

			// build XML DOM of users
			List<Element> users = toDomUsers();
			Element rootElement = DocumentFactory.getInstance().createElement(XmlConstants.XML_USERS);
			for (Element userElement : users) {
				rootElement.add(userElement);
			}

			// write DOM to file
			XmlHelper.writeDocument(rootElement, usersFile);
			this.userMapDirty = true;
		}

		// ROLES
		// get roles file name
		String rolesFileName = this.parameterMap.get(XmlConstants.XML_PARAM_ROLES_FILE);
		if (rolesFileName == null || rolesFileName.isEmpty()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_ROLES_FILE + " is invalid");
		}
		// get roles file
		File rolesFile = new File(this.basePath + "/" + rolesFileName);
		boolean rolesFileUnchanged = rolesFile.exists() && rolesFile.lastModified() == this.rolesFileDate;
		if (!this.roleMapDirty && rolesFileUnchanged) {
			logger.warn("No roles unpersisted and roles file unchanged on file system");
		} else {
			logger.info("Persisting roles...");

			// build XML DOM of roles
			List<Element> roles = toDomRoles();
			Element rootElement = DocumentFactory.getInstance().createElement(XmlConstants.XML_ROLES);
			for (Element roleElement : roles) {
				rootElement.add(roleElement);
			}

			// write DOM to file
			XmlHelper.writeDocument(rootElement, rolesFile);
			this.roleMapDirty = true;
		}

		// PRIVILEGES
		// get privileges file name
		String privilegesFileName = this.parameterMap.get(XmlConstants.XML_PARAM_PRIVILEGES_FILE);
		if (privilegesFileName == null || privilegesFileName.isEmpty()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_PRIVILEGES_FILE + " is invalid");
		}
		// get privileges file
		File privilegesFile = new File(this.basePath + "/" + privilegesFileName);
		boolean privilegesFileUnchanged = privilegesFile.exists()
				&& privilegesFile.lastModified() == this.privilegesFileDate;
		if (!this.privilegeMapDirty && privilegesFileUnchanged) {
			logger.warn("No privileges unpersisted and privileges file unchanged on file system");
		} else {
			logger.info("Persisting privileges...");

			// build XML DOM of privileges
			List<Element> privileges = toDomPrivileges();
			Element rootElement = DocumentFactory.getInstance().createElement(XmlConstants.XML_PRIVILEGES);
			for (Element privilegeElement : privileges) {
				rootElement.add(privilegeElement);
			}

			// write DOM to file
			XmlHelper.writeDocument(rootElement, privilegesFile);
			this.privilegeMapDirty = true;
		}

		// reset dirty states and return if something was dirty, false otherwise
		if (this.userMapDirty || this.roleMapDirty || this.privilegeMapDirty) {
			this.userMapDirty = false;
			this.roleMapDirty = false;
			this.privilegeMapDirty = false;

			return true;

		}

		this.userMapDirty = false;
		this.roleMapDirty = false;
		this.privilegeMapDirty = false;

		return false;
	}

	/**
	 * @see ch.eitchnet.privilege.handler.EncryptionHandler#initialize(java.util.Map)
	 */
	@Override
	public void initialize(Map<String, String> parameterMap) {

		this.roleMap = new HashMap<String, Role>();
		this.userMap = new HashMap<String, User>();
		this.privilegeMap = new HashMap<String, Privilege>();
		this.policyMap = new HashMap<String, Class<PrivilegePolicy>>();

		// get and validate base bath
		this.basePath = parameterMap.get(XmlConstants.XML_PARAM_BASE_PATH);
		File basePathF = new File(this.basePath);
		if (!basePathF.exists() && !basePathF.isDirectory()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_BASE_PATH + " is invalid");
		}

		// ROLES
		// get roles file name
		String rolesFileName = parameterMap.get(XmlConstants.XML_PARAM_ROLES_FILE);
		if (rolesFileName == null || rolesFileName.isEmpty()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_ROLES_FILE + " is invalid");
		}

		// get roles file
		File rolesFile = new File(this.basePath + "/" + rolesFileName);
		if (!rolesFile.exists()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_ROLES_FILE + " is invalid as roles file does not exist at path "
					+ rolesFile.getAbsolutePath());
		}

		// parse roles xml file to XML document
		Element rolesRootElement = XmlHelper.parseDocument(rolesFile).getRootElement();

		// read roles
		readRoles(rolesRootElement);
		this.rolesFileDate = rolesFile.lastModified();

		// USERS
		// get users file name
		String usersFileName = parameterMap.get(XmlConstants.XML_PARAM_USERS_FILE);
		if (usersFileName == null || usersFileName.isEmpty()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_USERS_FILE + " is invalid");
		}

		// get users file
		File usersFile = new File(this.basePath + "/" + usersFileName);
		if (!usersFile.exists()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_USERS_FILE + " is invalid as users file does not exist at path "
					+ usersFile.getAbsolutePath());
		}

		// parse users xml file to XML document
		Element usersRootElement = XmlHelper.parseDocument(usersFile).getRootElement();

		// read users
		readUsers(usersRootElement);
		this.usersFileDate = usersFile.lastModified();

		// PRIVILEGES
		// get privileges file name
		String privilegesFileName = parameterMap.get(XmlConstants.XML_PARAM_PRIVILEGES_FILE);
		if (privilegesFileName == null || privilegesFileName.isEmpty()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_PRIVILEGES_FILE + " is invalid");
		}

		// get privileges file
		File privilegesFile = new File(this.basePath + "/" + privilegesFileName);
		if (!privilegesFile.exists()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_PRIVILEGES_FILE + " is invalid as privileges file does not exist at path "
					+ privilegesFile.getAbsolutePath());
		}

		// parse privileges xml file to XML document
		Element privilegesRootElement = XmlHelper.parseDocument(privilegesFile).getRootElement();

		// read privileges
		readPrivileges(privilegesRootElement);
		this.privilegesFileDate = privilegesFile.lastModified();

		// POLICIES
		// get policy file name
		String policyFileName = parameterMap.get(XmlConstants.XML_PARAM_POLICY_FILE);
		if (policyFileName == null || policyFileName.isEmpty()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_POLICY_FILE + " is invalid");
		}

		// get policy file
		File policyFile = new File(this.basePath + "/" + policyFileName);
		if (!policyFile.exists()) {
			throw new PrivilegeException("[" + PersistenceHandler.class.getName() + "] Defined parameter "
					+ XmlConstants.XML_PARAM_POLICY_FILE + " is invalid as policy file does not exist at path "
					+ policyFile.getAbsolutePath());
		}

		// parse policy xml file to XML document
		Element policiesRootElement = XmlHelper.parseDocument(policyFile).getRootElement();

		// read policies
		readPolicies(policiesRootElement);

		this.userMapDirty = false;
		this.roleMapDirty = false;
		this.privilegeMapDirty = false;

		logger.info("Read " + this.userMap.size() + " Users");
		logger.info("Read " + this.roleMap.size() + " Roles");
		logger.info("Read " + this.privilegeMap.size() + " Privileges");

		// validate we have a user with PrivilegeAdmin access
		boolean privilegeAdminExists = false;
		for (String username : this.userMap.keySet()) {
			User user = this.userMap.get(username);
			if (user.hasRole(PrivilegeHandler.PRIVILEGE_ADMIN_ROLE)) {
				privilegeAdminExists = true;
				break;
			}
		}
		if (!privilegeAdminExists) {
			logger.warn("No User with PrivilegeAdmin role exists. Privilege modifications will not be possible!");
		}
	}

	/**
	 * @param usersRootElement
	 */
	private void readUsers(Element usersRootElement) {

		@SuppressWarnings("unchecked")
		List<Element> userElements = usersRootElement.elements(XmlConstants.XML_USER);
		for (Element userElement : userElements) {

			String userId = userElement.attributeValue(XmlConstants.XML_ATTR_USER_ID);

			String username = userElement.attributeValue(XmlConstants.XML_ATTR_USERNAME);
			String password = userElement.attributeValue(XmlConstants.XML_ATTR_PASSWORD);

			String firstname = userElement.element(XmlConstants.XML_FIRSTNAME).getTextTrim();
			String surname = userElement.element(XmlConstants.XML_SURNAME).getTextTrim();

			UserState userState = UserState.valueOf(userElement.element(XmlConstants.XML_STATE).getTextTrim());

			// TODO better parsing needed
			String localeName = userElement.element(XmlConstants.XML_LOCALE).getTextTrim();
			Locale locale = new Locale(localeName);

			Element rolesElement = userElement.element(XmlConstants.XML_ROLES);
			@SuppressWarnings("unchecked")
			List<Element> rolesElementList = rolesElement.elements(XmlConstants.XML_ROLE);
			Set<String> roles = new HashSet<String>();
			for (Element roleElement : rolesElementList) {
				String roleName = roleElement.getTextTrim();
				if (roleName.isEmpty()) {
					logger.error("User " + username + " has a role defined with no name, Skipped.");
				} else {
					roles.add(roleName);
				}
			}

			// create user
			User user = new User(userId, username, password, firstname, surname, userState,
					Collections.unmodifiableSet(roles), locale);

			// put user in map
			this.userMap.put(username, user);
		}
	}

	/**
	 * @param rolesRootElement
	 */
	private void readRoles(Element rolesRootElement) {

		@SuppressWarnings("unchecked")
		List<Element> roleElements = rolesRootElement.elements(XmlConstants.XML_ROLE);
		for (Element roleElement : roleElements) {

			String roleName = roleElement.attributeValue(XmlConstants.XML_ATTR_NAME);

			@SuppressWarnings("unchecked")
			List<Element> privilegeElements = roleElement.elements(XmlConstants.XML_PRIVILEGE);
			Set<String> privileges = new HashSet<String>();
			for (Element privilegeElement : privilegeElements) {

				String privilegeName = privilegeElement.attributeValue(XmlConstants.XML_ATTR_NAME);
				privileges.add(privilegeName);
			}

			Role role = new Role(roleName, privileges);
			this.roleMap.put(roleName, role);
		}
	}

	/**
	 * @param rolesRootElement
	 */
	private void readPrivileges(Element privilegesRootElement) {

		@SuppressWarnings("unchecked")
		List<Element> privilegeElements = privilegesRootElement.elements(XmlConstants.XML_PRIVILEGE);
		for (Element privilegeElement : privilegeElements) {

			String privilegeName = privilegeElement.attributeValue(XmlConstants.XML_ATTR_NAME);
			String privilegePolicy = privilegeElement.attributeValue(XmlConstants.XML_ATTR_POLICY);

			String allAllowedS = privilegeElement.element(XmlConstants.XML_ALL_ALLOWED).getTextTrim();
			boolean allAllowed = Boolean.valueOf(allAllowedS).booleanValue();

			@SuppressWarnings("unchecked")
			List<Element> denyElements = privilegeElement.elements(XmlConstants.XML_DENY);
			Set<String> denyList = new HashSet<String>(denyElements.size());
			for (Element denyElement : denyElements) {
				String denyValue = denyElement.getTextTrim();
				if (denyValue.isEmpty()) {
					logger.error("Privilege " + privilegeName + " has an empty deny value!");
				} else {
					denyList.add(denyValue);
				}
			}

			@SuppressWarnings("unchecked")
			List<Element> allowElements = privilegeElement.elements(XmlConstants.XML_ALLOW);
			Set<String> allowList = new HashSet<String>(allowElements.size());
			for (Element allowElement : allowElements) {
				String allowValue = allowElement.getTextTrim();
				if (allowValue.isEmpty()) {
					logger.error("Privilege " + privilegeName + " has an empty allow value!");
				} else {
					allowList.add(allowValue);
				}
			}

			Privilege privilege = new Privilege(privilegeName, privilegePolicy, allAllowed, denyList, allowList);
			this.privilegeMap.put(privilegeName, privilege);
		}
	}

	/**
	 * @param policiesRootElement
	 */
	private void readPolicies(Element policiesRootElement) {

		@SuppressWarnings("unchecked")
		List<Element> policyElements = policiesRootElement.elements(XmlConstants.XML_POLICY);
		for (Element policyElement : policyElements) {
			String policyName = policyElement.attributeValue(XmlConstants.XML_ATTR_NAME);
			String policyClass = policyElement.attributeValue(XmlConstants.XML_ATTR_CLASS);

			Class<PrivilegePolicy> clazz = ClassHelper.loadClass(policyClass);

			this.policyMap.put(policyName, clazz);
		}
	}

	private List<Element> toDomPrivileges() {

		List<Element> privilegesAsElements = new ArrayList<Element>(this.privilegeMap.size());

		DocumentFactory documentFactory = DocumentFactory.getInstance();
		for (String privilegeName : this.privilegeMap.keySet()) {

			// get the privilege object
			Privilege privilege = this.privilegeMap.get(privilegeName);

			// create the privilege element
			Element privilegeElement = documentFactory.createElement(XmlConstants.XML_PRIVILEGE);
			privilegeElement.addAttribute(XmlConstants.XML_ATTR_NAME, privilege.getName());
			privilegeElement.addAttribute(XmlConstants.XML_ATTR_POLICY, privilege.getPolicy());

			// add the all allowed element
			Element allAllowedElement = documentFactory.createElement(XmlConstants.XML_ALL_ALLOWED);
			allAllowedElement.setText(Boolean.toString(privilege.isAllAllowed()));
			privilegeElement.add(allAllowedElement);

			// add all the deny values
			for (String denyValue : privilege.getDenyList()) {
				Element denyValueElement = documentFactory.createElement(XmlConstants.XML_DENY);
				denyValueElement.setText(denyValue);
				privilegeElement.add(denyValueElement);
			}

			// add all the allow values
			for (String allowValue : privilege.getAllowList()) {
				Element allowValueElement = documentFactory.createElement(XmlConstants.XML_ALLOW);
				allowValueElement.setText(allowValue);
				privilegeElement.add(allowValueElement);
			}

			// add element to return list
			privilegesAsElements.add(privilegeElement);
		}

		return privilegesAsElements;
	}

	private List<Element> toDomRoles() {

		List<Element> rolesAsElements = new ArrayList<Element>(this.roleMap.size());

		DocumentFactory documentFactory = DocumentFactory.getInstance();
		for (String roleName : this.roleMap.keySet()) {

			// get the role object
			Role role = this.roleMap.get(roleName);

			// create the role element
			Element roleElement = documentFactory.createElement(XmlConstants.XML_ROLE);
			roleElement.addAttribute(XmlConstants.XML_ATTR_NAME, role.getName());

			// add all the privileges
			for (String privilegeName : role.getPrivileges()) {
				Element privilegeElement = documentFactory.createElement(XmlConstants.XML_PRIVILEGE);
				privilegeElement.addAttribute(XmlConstants.XML_ATTR_NAME, privilegeName);
				roleElement.add(privilegeElement);
			}

			// add element to return list
			rolesAsElements.add(roleElement);
		}

		return rolesAsElements;
	}

	private List<Element> toDomUsers() {

		List<Element> usersAsElements = new ArrayList<Element>(this.userMap.size());

		DocumentFactory documentFactory = DocumentFactory.getInstance();
		for (String userName : this.userMap.keySet()) {

			// get the user object
			User user = this.userMap.get(userName);

			// create the user element
			Element userElement = documentFactory.createElement(XmlConstants.XML_USER);
			userElement.addAttribute(XmlConstants.XML_ATTR_USER_ID, user.getUserId());
			userElement.addAttribute(XmlConstants.XML_ATTR_USERNAME, user.getUsername());
			userElement.addAttribute(XmlConstants.XML_ATTR_PASSWORD, user.getPassword());

			// add first name element
			Element firstnameElement = documentFactory.createElement(XmlConstants.XML_FIRSTNAME);
			firstnameElement.setText(user.getFirstname());
			userElement.add(firstnameElement);

			// add surname element
			Element surnameElement = documentFactory.createElement(XmlConstants.XML_SURNAME);
			surnameElement.setText(user.getSurname());
			userElement.add(surnameElement);

			// add state element
			Element stateElement = documentFactory.createElement(XmlConstants.XML_STATE);
			stateElement.setText(user.getUserState().toString());
			userElement.add(stateElement);

			// add locale element
			Element localeElement = documentFactory.createElement(XmlConstants.XML_LOCALE);
			localeElement.setText(user.getLocale().toString());
			userElement.add(localeElement);

			// add all the role elements
			Element rolesElement = documentFactory.createElement(XmlConstants.XML_ROLES);
			userElement.add(rolesElement);
			for (String roleName : user.getRoles()) {
				Element roleElement = documentFactory.createElement(XmlConstants.XML_ROLE);
				roleElement.setText(roleName);
				rolesElement.add(roleElement);
			}

			// add element to return list
			usersAsElements.add(userElement);
		}

		return usersAsElements;
	}
}
