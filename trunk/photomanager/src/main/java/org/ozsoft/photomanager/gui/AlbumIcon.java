package org.ozsoft.photomanager.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.ozsoft.photomanager.entities.Album;
import org.ozsoft.photomanager.entities.Photo;

public class AlbumIcon extends JPanel implements MouseListener {
    
    private static final long serialVersionUID = -2347960330864112521L;
    
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("d MMM. yyyy");
    
    private static final Color SELECTION_COLOR = new Color(255, 255, 128);
    
    private static final Color DEFAULT_COLOR = Color.WHITE;
    
    private static final Font NORMAL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    
    private static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    
    private static final String DEFAULT_ALBUM_IMAGE = "/images/album.png";
    
    private final AlbumListener parent;
    
    private final Album album;
    
    private final JLabel thumbnailLabel;
    
    private final JLabel nameLabel;
    
    private final JLabel dateLabel;
    
    private boolean isSelected = false;

    public AlbumIcon(Album album, AlbumListener parent) {
        this.album = album;
        this.parent = parent;
        
        setBackground(DEFAULT_COLOR);
        setBorder(new LineBorder(Color.LIGHT_GRAY));

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Thumbnail of album's cover photo.
        thumbnailLabel = new JLabel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(thumbnailLabel, gbc);

        nameLabel = new JLabel();
        nameLabel.setFont(NORMAL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(0, 5, 0, 5);
        add(nameLabel, gbc);

        dateLabel = new JLabel();
        dateLabel.setFont(SMALL_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(0, 5, 5, 5);
        add(dateLabel, gbc);
        
        Photo coverPhoto = album.getCoverPhoto();
        if (coverPhoto != null) {
            thumbnailLabel.setIcon(new ImageIcon(coverPhoto.getThumbnail()));
        } else {
            // No cover photo set; use placeholder icon.
            thumbnailLabel.setIcon(new ImageIcon(getClass().getResource(DEFAULT_ALBUM_IMAGE)));
        }
        
        String name = album.getName();
        if (name != null) {
            nameLabel.setText(name);
        }

        dateLabel.setText(DATE_FORMAT.format(album.getDate()));
        
        addMouseListener(this);
    }
    
    public void unselect() {
        isSelected = false;
        setBackground(DEFAULT_COLOR);
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.getClickCount() == 1) {
                if (!isSelected) {
                    parent.albumSelected(album);
                    isSelected = true;
                    setBackground(SELECTION_COLOR);
                }
            } else if (e.getClickCount() == 2) {
                unselect();
                parent.albumOpened(album);
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Not implemented.
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Not implemented.
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Not implemented.
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Not implemented.
    }

}