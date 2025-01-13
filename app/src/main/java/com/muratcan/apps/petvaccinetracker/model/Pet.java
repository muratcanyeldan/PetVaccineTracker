package com.muratcan.apps.petvaccinetracker.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    
    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";
    
    @NonNull
    @ColumnInfo(name = "type")
    private String type = "";
    
    @NonNull
    @ColumnInfo(name = "breed")
    private String breed = "Unknown";
    
    @NonNull
    @TypeConverters(DateConverter.class)
    @ColumnInfo(name = "birth_date")
    private Date birthDate = new Date();
    
    @Nullable
    @ColumnInfo(name = "image_uri")
    private String imageUri;
    
    @NonNull
    @ColumnInfo(name = "user_id")
    private String userId = "default_user";

    // Default constructor for Room
    public Pet() {
    }

    // Constructor for creating new pets
    @Ignore
    public Pet(String name, String type, String breed, Date birthDate, Uri imageUri, String userId) {
        this.name = name != null ? name : "";
        this.type = type != null ? type : "";
        this.breed = breed != null ? breed : "Unknown";
        this.birthDate = birthDate != null ? birthDate : new Date();
        this.imageUri = imageUri != null ? imageUri.toString() : null;
        this.userId = userId != null ? userId : "default_user";
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

    @NonNull
    public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name != null ? name : ""; }

    @NonNull
    public String getType() { return type; }
    public void setType(@NonNull String type) { this.type = type != null ? type : ""; }

    @NonNull
    public String getBreed() { return breed; }
    public void setBreed(@NonNull String breed) { this.breed = breed != null ? breed : "Unknown"; }

    @NonNull
    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(@NonNull Date birthDate) { this.birthDate = birthDate != null ? birthDate : new Date(); }

    @Nullable
    public String getImageUri() { return imageUri; }
    public void setImageUri(@Nullable String imageUri) { this.imageUri = imageUri; }

    @NonNull
    public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId != null ? userId : "default_user"; }
} 