/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.webui.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

/**
 * Displays the list of all existing users and their information. By using this UIAllPeople component, users could manage
 * his connections: invite to connect, revoke invitations, validate invited requests or remove connections.
 * 
 * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
 * @since Aug 25, 2011
 * @since 1.2.2
 */
@ComponentConfig(
  template = "classpath:groovy/social/webui/connections/UIAllPeople.gtmpl",
  events = {
    @EventConfig(listeners = UIDisplayProfileList.ConnectActionListener.class),
    @EventConfig(listeners = UIDisplayProfileList.ConfirmActionListener.class),
    @EventConfig(listeners = UIDisplayProfileList.IgnoreActionListener.class),
    @EventConfig(listeners = UIDisplayProfileList.SearchActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIDisplayProfileList.LoadMorePeopleActionListener.class)
  }
)
public class UIDisplayProfileList extends UIContainer {
  
  private static final Log LOG = ExoLogger.getLogger(UIDisplayProfileList.class);
  
  /**
   * Label for display invoke action
   */
  private static final String INVITATION_REVOKED_INFO = "UIDisplayProfileList.label.RevokedInfo"; 

  /**
   * Label for display established invitation
   */
  private static final String INVITATION_ESTABLISHED_INFO = "UIDisplayProfileList.label.InvitationEstablishedInfo";

  /**
   * Number element per page.
   */
  private static final Integer PEOPLE_PER_PAGE = 45;

  /**
   * The search object variable.
   */
  UIProfileUserSearch uiProfileUserSearch = null;

  private boolean loadAtEnd = false;
  private boolean hasUpdated = false;
  private int currentLoadIndex;
  private boolean enableLoadNext;
  private int loadingCapacity;
  private List<Identity> peopleList;
  private ListAccess<Identity> peopleListAccess;
  private int peopleNum;
  
  /**
   * Constructor to initialize iterator.
   *
   * @throws Exception
   */
  public UIDisplayProfileList() throws Exception {
    uiProfileUserSearch = addChild(UIProfileUserSearch.class, null, null);
    uiProfileUserSearch.setHasPeopleTab(false);
    uiProfileUserSearch.setHasConnectionLink(true);
    init();
  }
  
  /**
   * Inits at the first loading.
   */
  public void init() {
    try {
      setHasUpdatedIdentity(false);
      setLoadAtEnd(false);
      enableLoadNext = false;
      currentLoadIndex = 0;
      loadingCapacity = PEOPLE_PER_PAGE;
      peopleList = new ArrayList<Identity>();
      List<Identity> excludedIdentityList = new ArrayList<Identity>();
      excludedIdentityList.add(Utils.getViewerIdentity());
      uiProfileUserSearch.getProfileFilter().setExcludedIdentityList(excludedIdentityList);
      setPeopleList(loadPeople(currentLoadIndex, loadingCapacity));
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }
  
  /**
   * Sets loading capacity.
   * 
   * @param loadingCapacity
   */
  public void setLoadingCapacity(int loadingCapacity) {
    this.loadingCapacity = loadingCapacity;
  }

  /**
   * Gets flag to display LoadNext button or not.
   * 
   * @return the enableLoadNext
   */
  public boolean isEnableLoadNext() {
    return enableLoadNext;
  }

  /**
   * Sets flag to display LoadNext button or not.
   * 
   * @param enableLoadNext the enableLoadNext to set
   */
  public void setEnableLoadNext(boolean enableLoadNext) {
    this.enableLoadNext = enableLoadNext;
  }

  /**
   * Gets flags to clarify that load at the last element or not. 
   * 
   * @return the loadAtEnd
   */
  public boolean isLoadAtEnd() {
    return loadAtEnd;
  }

  /**
   * Sets flags to clarify that load at the last element or not.
   * 
   * @param loadAtEnd the loadAtEnd to set
   */
  public void setLoadAtEnd(boolean loadAtEnd) {
    this.loadAtEnd = loadAtEnd;
  }

  /**
   * Gets information that clarify one element is updated or not.
   * 
   * @return the hasUpdatedIdentity
   */
  public boolean isHasUpdatedIdentity() {
    return hasUpdated;
  }

  /**
   * Sets information that clarify one element is updated or not.
   * 
   * @param hasUpdatedIdentity the hasUpdatedIdentity to set
   */
  public void setHasUpdatedIdentity(boolean hasUpdatedIdentity) {
    this.hasUpdated = hasUpdatedIdentity;
  }

  /**
   * Gets list of all type of people.
   * 
   * @return the peopleList
   * @throws Exception 
   */
  public List<Identity> getPeopleList() throws Exception {
    if (isHasUpdatedIdentity()) {
      setHasUpdatedIdentity(false);
      setPeopleList(loadPeople(0, this.peopleList.size()));
    }
    
    int realPeopleListSize = this.peopleList.size();

    setEnableLoadNext((realPeopleListSize >= PEOPLE_PER_PAGE)
            && (realPeopleListSize < getPeopleNum()));
    
    return this.peopleList;
  }

  /**
   * Sets list of all type of people.
   * 
   * @param peopleList the peopleList to set
   */
  public void setPeopleList(List<Identity> peopleList) {
    this.peopleList = peopleList;
  }
  
  /**
   * Gets number of people for displaying.
   * 
   * @return the peopleNum
   */
  public int getPeopleNum() {
    return peopleNum;
  }

  /**
   * Sets number of people for displaying.
   * @param peopleNum the peopleNum to set
   */
  public void setPeopleNum(int peopleNum) {
    this.peopleNum = peopleNum;
  }

  /**
   * Gets people with ListAccess type.
   * 
   * @return the peopleListAccess
   */
  public ListAccess<Identity> getPeopleListAccess() {
    return peopleListAccess;
  }

  /**
   * Sets people with ListAccess type.
   * 
   * @param peopleListAccess the peopleListAccess to set
   */
  public void setPeopleListAccess(ListAccess<Identity> peopleListAccess) {
    this.peopleListAccess = peopleListAccess;
  }

  /**
   * Loads more people.
   * @throws Exception
   */
  public void loadNext() throws Exception {
    currentLoadIndex += loadingCapacity;
    if (currentLoadIndex <= getPeopleNum()) {
      List<Identity> currentPeopleList = new ArrayList<Identity>(this.peopleList);
      List<Identity> loadedPeople = new ArrayList<Identity>(Arrays.asList(getPeopleListAccess()
                    .load(currentLoadIndex, loadingCapacity)));
      currentPeopleList.addAll(loadedPeople);
      setPeopleList(currentPeopleList);
    }
  }
  
  /**
   * Loads people when searching.
   * @throws Exception
   */
  public void loadSearch() throws Exception {
    currentLoadIndex = 0;
    setPeopleList(loadPeople(currentLoadIndex, loadingCapacity));
  }
  
  private List<Identity> loadPeople(int index, int length) throws Exception {
    ProfileFilter filter = uiProfileUserSearch.getProfileFilter();
    setPeopleListAccess(Utils.getIdentityManager().getIdentitiesByProfileFilter(
            OrganizationIdentityProvider.NAME, filter, true));
    
    setPeopleNum(getPeopleListAccess().getSize());
    uiProfileUserSearch.setPeopleNum(getPeopleNum());
    Identity[] people = getPeopleListAccess().load(index, length);

//  This is the lack of API, filter by code is not good, that's the reason why we commented these lines.    
//    if (filter.getSkills().length() > 0) { 
//      return uiProfileUserSearch.getIdentitiesBySkills(
//        new ArrayList<Identity>(Arrays.asList(people)));
//    }

    return new ArrayList<Identity>(Arrays.asList(people));
  }
  
  /**
   * Listeners loading more people action.
   * 
   * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
   * @since Aug 18, 2011
   */
  static public class LoadMorePeopleActionListener extends EventListener<UIDisplayProfileList> {
    public void execute(Event<UIDisplayProfileList> event) throws Exception {
      UIDisplayProfileList uiAllPeople = event.getSource();
      if (uiAllPeople.currentLoadIndex < uiAllPeople.peopleNum) {
        uiAllPeople.loadNext();
      } else {
        uiAllPeople.setEnableLoadNext(false);
      }
    }
  }
  
  /**
   * Listens to add action then make request to invite person to make connection.<br> - Gets
   * information of user is invited.<br> - Checks the relationship to confirm that there have not
   * got connection yet.<br> - Saves the new connection.<br>
   */
  public static class ConnectActionListener extends EventListener<UIDisplayProfileList> {
    public void execute(Event<UIDisplayProfileList> event) throws Exception {
      UIDisplayProfileList uiAllPeople = event.getSource();
      String userId = event.getRequestContext().getRequestParameter(OBJECTID);
      Identity invitedIdentity = Utils.getIdentityManager().getIdentity(userId, true);
      Identity invitingIdentity = Utils.getViewerIdentity();

      Relationship relationship = Utils.getRelationshipManager().get(invitingIdentity, invitedIdentity);
      uiAllPeople.setLoadAtEnd(false);
      
      if (relationship != null) {
        UIApplication uiApplication = event.getRequestContext().getUIApplication();
        uiApplication.addMessage(new ApplicationMessage(INVITATION_ESTABLISHED_INFO, null, ApplicationMessage.INFO));
        return;
      }
      
      uiAllPeople.setHasUpdatedIdentity(true);
      Utils.getRelationshipManager().inviteToConnect(invitingIdentity, invitedIdentity);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiAllPeople);
    }
  }

  /**
   * Listens to accept actions then make connection to accepted person.<br> - Gets information of
   * user who made request.<br> - Checks the relationship to confirm that there still got invited
   * connection.<br> - Makes and Save the new relationship.<br>
   */
  public static class ConfirmActionListener extends EventListener<UIDisplayProfileList> {
    public void execute(Event<UIDisplayProfileList> event) throws Exception {
      UIDisplayProfileList uiAllPeople = event.getSource();
      String userId = event.getRequestContext().getRequestParameter(OBJECTID);
      Identity invitedIdentity = Utils.getIdentityManager().getIdentity(userId, true);
      Identity invitingIdentity = Utils.getViewerIdentity();

      Relationship relationship = Utils.getRelationshipManager().get(invitingIdentity, invitedIdentity);
      uiAllPeople.setLoadAtEnd(false);
      
      if (relationship == null || relationship.getStatus() != Relationship.Type.PENDING) {
        UIApplication uiApplication = event.getRequestContext().getUIApplication();
        uiApplication.addMessage(new ApplicationMessage(INVITATION_REVOKED_INFO, null, ApplicationMessage.INFO));
        return;
      }
      
      uiAllPeople.setHasUpdatedIdentity(true);
      Utils.getRelationshipManager().confirm(invitedIdentity, invitingIdentity);
    }
  }

  /**
   * Listens to deny action then delete the invitation.<br> - Gets information of user is invited or
   * made request.<br> - Checks the relation to confirm that there have not got relation yet.<br> -
   * Removes the current relation and save the new relation.<br>
   */
  public static class IgnoreActionListener extends EventListener<UIDisplayProfileList> {
    public void execute(Event<UIDisplayProfileList> event) throws Exception {
      UIDisplayProfileList   uiAllPeople = event.getSource();
      String userId = event.getRequestContext().getRequestParameter(OBJECTID);
      Identity inviIdentityIdentity = Utils.getIdentityManager().getIdentity(userId, true);
      Identity invitingIdentity = Utils.getViewerIdentity();

      Relationship relationship = Utils.getRelationshipManager().get(invitingIdentity, inviIdentityIdentity);
      
      uiAllPeople.setLoadAtEnd(false);
      if (relationship != null && relationship.getStatus() == Relationship.Type.CONFIRMED) {
        Utils.getRelationshipManager().delete(relationship);
        return;
      }
      
      if (relationship == null) {
        UIApplication uiApplication = event.getRequestContext().getUIApplication();
        uiApplication.addMessage(new ApplicationMessage(INVITATION_REVOKED_INFO, null, ApplicationMessage.INFO));
        return;
      }
      
      uiAllPeople.setHasUpdatedIdentity(true);
      Utils.getRelationshipManager().deny(inviIdentityIdentity, invitingIdentity);
    }
  }

  /**
   * Listens event that broadcast from UIProfileUserSearch.
   * 
   * @author <a href="mailto:hanhvq@exoplatform.com">Hanh Vi Quoc</a>
   * @since Aug 25, 2011
   */
  static public class SearchActionListener extends EventListener<UIDisplayProfileList> {
    @Override
    public void execute(Event<UIDisplayProfileList> event) throws Exception {
      UIDisplayProfileList uiAllPeople = event.getSource();
      uiAllPeople.loadSearch();
      uiAllPeople.setLoadAtEnd(false);
    }
  }

  /**
   * @param identity
   * @return
   * @throws Exception
   */
  public Relationship getRelationship(Identity identity) throws Exception {
    if (identity.equals(Utils.getViewerIdentity())) {
      return null;
    }
    return Utils.getRelationshipManager().get(identity, Utils.getViewerIdentity());
  }

}
