/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.jinglenote;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

@ComponentInformation(friendlyName = "JingleNote", desc = "MIDI sequencer for note blocks with commands.")
public class JingleNoteComponent extends AbstractComponent implements Listener {

    private JingleNoteManager jingleNoteManager;

    @Override
    public void initialize() {
        // Jingle note manager
        jingleNoteManager = new JingleNoteManager();
        
        registerCommands(Commands.class);
        CommandBook.registerEvents(this);
    }

    @Override
    public void unload() {
        jingleNoteManager.stopAll();
    }

    /**
     * Get the jingle note manager.
     *
     * @return
     */
    public JingleNoteManager getJingleNoteManager() {
        return jingleNoteManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        MidiJingleSequencer sequencer = null;

        try {
            File file = new File(CommandBook.inst().getDataFolder(), "intro.mid");
            if (file.exists()) {
                sequencer = new MidiJingleSequencer(file);
                getJingleNoteManager().play(event.getPlayer(), sequencer, 2000);
            }
        } catch (MidiUnavailableException e) {
            CommandBook.logger().log(Level.WARNING, "Failed to access MIDI: "
                    + e.getMessage());
        } catch (InvalidMidiDataException e) {
            CommandBook.logger().log(Level.WARNING, "Failed to read intro MIDI file: "
                    + e.getMessage());
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            CommandBook.logger().log(Level.WARNING, "Failed to read intro MIDI file: "
                    + e.getMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        getJingleNoteManager().stop(event.getPlayer());
    }

    public class Commands {
        @Command(aliases = {"intro"},
                usage = "", desc = "Play the introduction song",
                min = 0, max = 0)
        @CommandPermissions({"commandbook.intro"})
        public void intro(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets;
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            }
            
            for (Player target : targets) {
                if (target != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.intro.other");
                    break;
                }
            }

            try {
                MidiJingleSequencer sequencer = new MidiJingleSequencer(
                        new File(CommandBook.inst().getDataFolder(), "intro.mid"));
                for (Player player : targets) {
                    getJingleNoteManager().play(player, sequencer, 0);
                    player.sendMessage(ChatColor.YELLOW + "Playing intro.midi...");
                }
            } catch (MidiUnavailableException e) {
                throw new CommandException("Failed to access MIDI: "
                        + e.getMessage());
            } catch (InvalidMidiDataException e) {
                throw new CommandException("Failed to read intro MIDI file: "
                        + e.getMessage());
            } catch (FileNotFoundException e) {
                throw new CommandException("No intro.mid is available.");
            } catch (IOException e) {
                throw new CommandException("Failed to read intro MIDI file: "
                        + e.getMessage());
            }
        }

        @Command(aliases = {"midi", "play"},
                usage = "[-p player] [midi]", desc = "Play a MIDI file", flags = "p:",
                min = 0, max = 1)
        public void midi(CommandContext args, CommandSender sender) throws CommandException {
            Iterable<Player> targets = null;
            if (args.hasFlag('p')) {
                targets = PlayerUtil.matchPlayers(sender, args.getFlag('p'));
            } else {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            }

            for (Player target : targets) {
                if (target != sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.midi.other");
                    break;
                }
            }

            if (args.argsLength() == 0) {
                for (Player target : targets) {
                    if (getJingleNoteManager().stop(target)) {
                        target.sendMessage(ChatColor.YELLOW + "All music stopped.");
                    }
                }
                return;
            }

            CommandBook.inst().checkPermission(sender, "commandbook.midi");

            String filename = args.getString(0);

            if (!filename.matches("^[A-Za-z0-9 \\-_\\.~\\[\\]\\(\\$),]+$")) {
                throw new CommandException("Invalid filename specified!");
            }

            File[] trialPaths = {
                    new File(CommandBook.inst().getDataFolder(), "midi/" + filename),
                    new File(CommandBook.inst().getDataFolder(), "midi/" + filename + ".mid"),
                    new File(CommandBook.inst().getDataFolder(), "midi/" + filename + ".midi"),
                    new File("midi", filename),
                    new File("midi", filename + ".mid"),
                    new File("midi", filename + ".midi"),
            };

            File file = null;

            for (File f : trialPaths) {
                if (f.exists()) {
                    file = f;
                    break;
                }
            }

            if (file == null) {
                throw new CommandException("The specified MIDI file was not found.");
            }

            try {
                MidiJingleSequencer sequencer = new MidiJingleSequencer(file);
                for (Player player : targets) {
                    getJingleNoteManager().play(player, sequencer, 0);
                    player.sendMessage(ChatColor.YELLOW + "Playing " + file.getName()
                            + "... Use '/midi' to stop.");
                }
            } catch (MidiUnavailableException e) {
                throw new CommandException("Failed to access MIDI: "
                        + e.getMessage());
            } catch (InvalidMidiDataException e) {
                throw new CommandException("Failed to read intro MIDI file: "
                        + e.getMessage());
            } catch (FileNotFoundException e) {
                throw new CommandException("The specified MIDI file was not found.");
            } catch (IOException e) {
                throw new CommandException("Failed to read intro MIDI file: "
                        + e.getMessage());
            }
        }
    }
}
