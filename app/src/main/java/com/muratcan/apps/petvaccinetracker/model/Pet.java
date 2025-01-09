package com.muratcan.apps.petvaccinetracker.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.muratcan.apps.petvaccinetracker.database.DateConverter;

import java.util.Date;

@Entity(tableName = "pets")
public class Pet implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "name")
    private String name;
    
    @ColumnInfo(name = "type")
    private String type;
    
    @ColumnInfo(name = "breed")
    private String breed;
    
    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "birth_date")
    private Date birthDate;
    
    @ColumnInfo(name = "image_uri")
    private String imageUri;
    
    @ColumnInfo(name = "user_id")
    private String userId;

    // Default constructor for Room
    public Pet() {
    }

    // Constructor for creating new pets
    @Ignore
    public Pet(String name, String type, String breed, Date birthDate, Uri imageUri, String userId) {
        this.name = name;
        this.type = type;
        this.breed = breed;
        this.birthDate = birthDate;
        this.imageUri = imageUri != null ? imageUri.toString() : null;
        this.userId = userId;
    }

    @Ignore
    protected Pet(Parcel in) {
        id = in.readLong();
        name = in.readString();
        type = in.readString();
        breed = in.readString();
        birthDate = new Date(in.readLong());
        imageUri = in.readString();
        userId = in.readString();
    }

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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(type);
        dest.writeString(breed);
        dest.writeLong(birthDate != null ? birthDate.getTime() : 0);
        dest.writeString(imageUri);
        dest.writeString(userId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
} 