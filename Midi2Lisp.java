// Make sure Cons class is in same folder

/* Created 11/13/16 by Juanito Taveras
Last modified: 11/13/16
Upload a MIDI file to this MIDI to text parser I found online:     http://valentin.dasdeck.com/midi/mid2txt.php
Then feed that text into this program to make music playable with jFugue using a Cons class.
 */

import java.io.*;
import java.util.*;

public class Midi2Lisp {

    public static Cons notes_table = Cons.readlist(Cons.list(
            "(0 C)",
            "(1 C#)",
            "(2 D)",
            "(3 D#)",
            "(4 E)",
            "(5 F)",
            "(6 F#)",
            "(7 G)",
            "(8 G#)",
            "(9 A)",
            "(10 A#)",
            "(11 B)"));

    // iterative reverse
    public static Cons reverse (Cons lst) {
        Cons lstb = null;
        while (lst != null) {
            lstb = Cons.cons(Cons.first(lst), lstb);
            lst = Cons.rest(lst);
        }
        return lstb;
    }

    public static void main(String[] args) throws Exception {
        // Check command line parameter usage
        if (args.length != 2) {
            System.out.println(
                    "Usage: java Midi2Lisp sourceFile targetFile");
            System.exit(1);
        }

        // Check if source file exists
        File source_file = new File(args[0]);
        if (!source_file.exists()) {
            System.out.println("Source file " + args[0] + " does not exist.");
            System.exit(2);
        }

        // Check if target file exists
        File target_file = new File(args[1]);
        if (target_file.exists()) {
            System.out.println("Target file " + args[1] + " already exists.");
            System.exit(3);
        }

        // TRACK SETTINGS // Set which channels you want converted to piano notes,
                          // and which tracks are the bass drum
        int[] piano_ch = {1, 2, 3};
        int[] bass_ch = {4};
        //////////////////////////////////////
        int lineidx = 0;
        int prev_dur = 0; // marks previous duration
        int cur_dur = 0; // marks current duration
        boolean track_head = false;
        boolean notes_read = false;
        boolean note_switch = false;
        Cons tracklist = null;
        Cons track = null;
        track = Cons.cons(track, tracklist);
        try (
                // Create input and output files
                Scanner input = new Scanner(source_file);
                PrintWriter output = new PrintWriter(target_file);
                ) {
            while (input.hasNextLine()) {
                String line = input.nextLine();  // read a line
                String[] words = line.split(" ");
                if (track_head) {
                    if (words.length == 5) {
                        if (words[2].length() > 3) {

                            if (!note_switch) {
                                // if note switch was off
                                if (words[1].equals("On")) {
                                    note_switch = true;
                                    prev_dur = Integer.parseInt(words[0]);
                                    int cur_rest = Integer.parseInt(words[0]);
                                    int duration = (cur_rest - cur_dur) / 32;
                                    //Cons rest = Cons.list("rest", (cur_rest - cur_dur) / 32);
                                    if (duration > 0) track = Cons.cons(Cons.list("rest", duration), track);
                                }
                            } else {
                                if (words[1].equals("Off")) {
                                    note_switch = false;
                                    cur_dur = Integer.parseInt(words[0]);
                                    int dur = (cur_dur - prev_dur) / 32;
                                    int ch = Integer.parseInt((words[2]).substring(3));
                                    // if it is a bass track
                                    for (int chn : bass_ch) {
                                        if (chn == ch) {
                                            track = Cons.cons(Cons.list("boom", dur), track);
                                        }
                                    }
                                    // if it is a piano track
                                    for (int chan : piano_ch) {
                                        if (chan == ch) {
                                            int note_num = Integer.parseInt((words[3]).substring(2));
                                            int octave = note_num / 12;
                                            int n = note_num % 12;
                                            String letter = (String) Cons.second(Cons.assoc(n, notes_table));
                                            letter += Integer.toString(octave);
                                            Cons note = Cons.list("piano", "0", letter, dur);
                                            if (note != null) track = Cons.cons(note, track);
                                        }
                                    }
                                }
                            }
                        }


                    }

                }

                if (words[0].equals("MTrk")) {  // indicates track start
                    track_head = true;
                    //track = null;
                } else if (words[0].equals("TrkEnd")) {
                    track_head = false;
                    track = Cons.cons("seq", reverse(track));
                    tracklist = Cons.cons(track, tracklist);
                    track = null;
                }

                lineidx++;
            }
            Cons tracklist_filtered = null;
            while (tracklist != null) {
                // remove any tracks that are too short (probably empty or non-MIDI tracks)
                if (Cons.length((Cons) Cons.first(tracklist)) > 10) {
                    tracklist_filtered = Cons.cons((Cons) Cons.first(tracklist), tracklist_filtered);
                }
                tracklist = Cons.rest(tracklist);
            }
            tracklist_filtered = Cons.cons("sync", tracklist_filtered);
            System.out.println("OUTPUT: " + " " + tracklist_filtered);
            output.println(tracklist_filtered);
        }
    }

}