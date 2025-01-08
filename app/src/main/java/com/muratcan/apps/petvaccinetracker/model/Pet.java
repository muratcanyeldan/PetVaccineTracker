package com.muratcan.apps.petvaccinetracker.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "pets")
public class Pet implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String type;
    private String breed;
    private String dateOfBirth;
    private String userId;

    public Pet() {
    }

    @Ignore
    public Pet(String name, String type, String breed, String dateOfBirth, String userId) {
        this.name = name;
        this.type = type;
        this.breed = breed;
        this.dateOfBirth = dateOfBirth;
        this.userId = userId;
    }

    @Ignore
    protected Pet(Parcel in) {
        id = in.readLong();
        name = in.readString();
        type = in.readString();
        breed = in.readString();
        dateOfBirth = in.readString();
        userId = in.readString();
    }

    @Ignore
    public static final Creator<Pet> CREATOR = new Creator<>() {
        @Override
        public Pet createFromParcel(Parcel in) {
            return new Pet(in);
        }

        @Override
        public Pet[] newArray(int size) {
            return new Pet[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(type);
        dest.writeString(breed);
        dest.writeString(dateOfBirth);
        dest.writeString(userId);
    }
} 