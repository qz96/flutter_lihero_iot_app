package com.lh.lhble.Entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * 用户模块显示权限单元
 */
public class Child implements Parcelable {
    private List<Child> children;
    private String component;
    private Meta meta;
    private String path;

    public List<Child> getChildren() {
        return children;
    }

    public void setChildren(List<Child> children) {
        this.children = children;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    @Override
    public String toString() {
        return "Child{" +
                "children=" + children +
                ", component='" + component + '\'' +
                ", meta=" + meta +
                ", path='" + path + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.children);
        dest.writeString(this.component);
        dest.writeParcelable(this.meta, flags);
        dest.writeString(this.path);
    }

    public Child() {
    }

    protected Child(Parcel in) {
        this.children = in.createTypedArrayList(Child.CREATOR);
        this.component = in.readString();
        this.meta = in.readParcelable(Meta.class.getClassLoader());
        this.path = in.readString();
    }

    public static final Creator<Child> CREATOR = new Creator<Child>() {
        @Override
        public Child createFromParcel(Parcel source) {
            return new Child(source);
        }

        @Override
        public Child[] newArray(int size) {
            return new Child[size];
        }
    };




    public static class Meta implements Parcelable{
        private int affix;
        private String title;
        private int type;

        public int getAffix() {
            return affix;
        }

        public void setAffix(int affix) {
            this.affix = affix;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.affix);
            dest.writeString(this.title);
            dest.writeInt(this.type);
        }

        public Meta() {
        }

        protected Meta(Parcel in) {
            this.affix = in.readInt();
            this.title = in.readString();
            this.type = in.readInt();
        }

        public static final Creator<Meta> CREATOR = new Creator<Meta>() {
            @Override
            public Meta createFromParcel(Parcel source) {
                return new Meta(source);
            }

            @Override
            public Meta[] newArray(int size) {
                return new Meta[size];
            }
        };
    }
}
