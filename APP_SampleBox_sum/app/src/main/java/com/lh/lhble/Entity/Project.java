package com.lh.lhble.Entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 用户模块显示权限单元
 */
public class Project implements Parcelable {
    private String id;
    private String projectName;
    private String projectCode;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.projectName);
        dest.writeString(this.projectCode);
    }

    public Project() {
    }

    protected Project(Parcel in) {
        this.id = in.readString();
        this.projectName = in.readString();
        this.projectCode = in.readString();
    }

    public static final Creator<Project> CREATOR = new Creator<Project>() {
        @Override
        public Project createFromParcel(Parcel source) {
            return new Project(source);
        }

        @Override
        public Project[] newArray(int size) {
            return new Project[size];
        }
    };
}
