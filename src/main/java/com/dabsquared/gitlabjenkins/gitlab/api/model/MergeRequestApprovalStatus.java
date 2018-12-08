package com.dabsquared.gitlabjenkins.gitlab.api.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class MergeRequestApprovalStatus {

    private Integer id;
    private Integer iid;
    private Integer projectId;

    private Boolean userCanApprove;
    private Boolean userHasApproved;

    // More attributes are available on approval status

    public MergeRequestApprovalStatus(){
        /* default-constructor for Resteasy-based-api-proxies */
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIid() {
        return iid;
    }

    public void setIid(Integer iid) {
        this.iid = iid;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Boolean getUserCanApprove() {
        return userCanApprove;
    }

    public void setUserCanApprove(Boolean userCanApprove) {
        this.userCanApprove = userCanApprove;
    }

    public Boolean getUserHasApproved() {
        return userHasApproved;
    }

    public void setUserHasApproved(Boolean userHasApproved) {
        this.userHasApproved = userHasApproved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MergeRequestApprovalStatus that = (MergeRequestApprovalStatus) o;
        return new EqualsBuilder()
            .append(id, that.id)
            .append(iid, that.iid)
            .append(projectId, that.projectId)
            .append(userCanApprove, that.userCanApprove)
            .append(userHasApproved, that.userHasApproved)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(iid)
            .append(projectId)
            .append(userCanApprove)
            .append(userHasApproved)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("iid", iid)
            .append("projectId", projectId)
            .append("userCanApprove", userCanApprove)
            .append("userHasApproved", userHasApproved)
            .toString();
    }
}
