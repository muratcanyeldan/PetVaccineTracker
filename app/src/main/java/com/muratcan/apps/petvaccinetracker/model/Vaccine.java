package com.muratcan.apps.petvaccinetracker.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "vaccines",
        indices = {@Index("petId")},
        foreignKeys = @ForeignKey(
            entity = Pet.class,
            parentColumns = "id",
            childColumns = "petId",
            onDelete = ForeignKey.CASCADE
        ))
public class Vaccine implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private long petId;
    private String dateAdministered;
    private String nextDueDate;
    private boolean isRecurring;
    private int recurringPeriodMonths;
    private String notes;

    public Vaccine() {
    }

    @Ignore
    public Vaccine(String name, long petId, String dateAdministered, String nextDueDate,
                  boolean isRecurring, int recurringPeriodMonths, String notes) {
        this.name = name;
        this.petId = petId;
        this.dateAdministered = dateAdministered;
        this.nextDueDate = nextDueDate;
        this.isRecurring = isRecurring;
        this.recurringPeriodMonths = recurringPeriodMonths;
        this.notes = notes;
    }

    @Ignore
    protected Vaccine(Parcel in) {
        id = in.readLong();
        name = in.readString();
        petId = in.readLong();
        dateAdministered = in.readString();
        nextDueDate = in.readString();
        isRecurring = in.readByte() != 0;
        recurringPeriodMonths = in.readInt();
        notes = in.readString();
    }

    @Ignore
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

    public long getPetId() {
        return petId;
    }

    public void setPetId(long petId) {
        this.petId = petId;
    }

    public String getDateAdministered() {
        return dateAdministered;
    }

    public void setDateAdministered(String dateAdministered) {
        this.dateAdministered = dateAdministered;
    }

    public String getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(String nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public int getRecurringPeriodMonths() {
        return recurringPeriodMonths;
    }

    public void setRecurringPeriodMonths(int recurringPeriodMonths) {
        this.recurringPeriodMonths = recurringPeriodMonths;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeLong(petId);
        dest.writeString(dateAdministered);
        dest.writeString(nextDueDate);
        dest.writeByte((byte) (isRecurring ? 1 : 0));
        dest.writeInt(recurringPeriodMonths);
        dest.writeString(notes);
    }
}