package org.parser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect

public class PrintNews {
    public String title;
    public String detailsLink;
    public String author;
    public String dateOfCreated;
    public String text;

    public void setText(String text) {
        this.text = text;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public void setDetailsLink(String detailsLink) {
        this.detailsLink = detailsLink;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    public void setDateOfCreated(String dateOfCreated) {
        this.dateOfCreated = dateOfCreated;
    }


    PrintNews(){}
}
