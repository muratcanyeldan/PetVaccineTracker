package com.muratcan.apps.petvaccinetracker.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "vaccines",
        foreignKeys = @ForeignKey(entity = Pet.class,
                                parentColumns = "id",
                                childColumns = "petId",
                                onDelete = ForeignKey.CASCADE),
        indices = {@Index("petId")})
public class Vaccine implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private Date dateAdministered;
    private Date nextDueDate;
    private String notes;
    private long petId;
    private boolean isRecurring;
    private int recurrenceMonths;

    public Vaccine() {
        this.isRecurring = false;
        this.recurrenceMonths = 0;
    }

    protected Vaccine(Parcel in) {
        id = in.readLong();
        name = in.readString();
        long dateAdministeredMillis = in.readLong();
        dateAdministered = dateAdministeredMillis != 0 ? new Date(dateAdministeredMillis) : null;
        long nextDueMillis = in.readLong();
        nextDueDate = nextDueMillis != 0 ? new Date(nextDueMillis) : null;
        notes = in.readString();
        petId = in.readLong();
        isRecurring = in.readByte() != 0;
        recurrenceMonths = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeLong(dateAdministered != null ? dateAdministered.getTime() : 0);
        dest.writeLong(nextDueDate != null ? nextDueDate.getTime() : 0);
        dest.writeString(notes);
        dest.writeLong(petId);
        dest.writeByte((byte) (isRecurring ? 1 : 0));
        dest.writeInt(recurrenceMonths);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Vaccine> CREATOR = new Creator<>() {
        @Override
        public Vaccine createFromParcel(Parcel in) {
            return new Vaccine(in);
        }

        @Override
        public Vaccine[] newArray(int size) {
            return new Vaccine[size];
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

    public Date getDateAdministered() {
        return dateAdministered;
    }

    public void setDateAdministered(Date dateAdministered) {
        this.dateAdministered = dateAdministered;
    }

    public Date getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(Date nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getPetId() {
        return petId;
    }

    public void setPetId(long petId) {
        this.petId = petId;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public int getRecurrenceMonths() {
        return recurrenceMonths;
    }

    public void setRecurrenceMonths(int recurrenceMonths) {
        this.recurrenceMonths = recurrenceMonths;
    }
}