package tf.bug.chalkbot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import net.time4j.Moment;
import net.time4j.format.DisplayMode;
import net.time4j.format.expert.ChronoFormatter;
import net.time4j.tz.Timezone;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tf.bug.chalkbot.ChalkBotClient;
import tf.bug.chalkbot.ChalkBotDB;

import java.text.MessageFormat;
import java.util.*;

public class CommandTime implements Command {

    private CommandTime() {}

    private static CommandTime instance;

    public static CommandTime getInstance() {
        if(instance == null) {
            instance = new CommandTime();
        }

        return instance;
    }

    @Override
    public boolean hasSubCommands() {
        return false;
    }

    @Override
    public boolean hasSubCommand(String subCommandToken) {
        return false;
    }

    @Override
    public Command getSubCommand(String subCommandName) {
        return null;
    }

    @Override
    public <T> Mono<T> run(ChalkBotClient client, MessageCreateEvent mce, Locale userLocale, String arguments) {
        return mce.getMessage().getChannel().flatMap(c -> {
            Mono<ChalkBotClient.MemberQueryResponse> target;
            if(arguments.isEmpty()) {
                Mono<Optional<Member>> member =
                    mce.getMessage().getAuthorAsMember().map(Optional::of).defaultIfEmpty(Optional.empty());
                target = member.map(o -> {
                    if(o.isPresent()) {
                        return new ChalkBotClient.MemberQueryResponse.Member(o.get());
                    } else {
                        Optional<User> user =
                            mce.getMessage().getAuthor();
                        if(user.isPresent()) {
                            return new ChalkBotClient.MemberQueryResponse.User(user.get());
                        } else {
                            return new ChalkBotClient.MemberQueryResponse.Empty();
                        }
                    }
                });
            } else {
                target = mce.getGuild().map(Optional::of).defaultIfEmpty(Optional.empty()).flatMap(g -> {
                    return client.queryMember(arguments, g);
                });
            }

            return target.flatMap(mqr -> {
                if(mqr instanceof ChalkBotClient.MemberQueryResponse.Member) {
                    Member targetMember = ((ChalkBotClient.MemberQueryResponse.Member) mqr).member();

                    Flux<Timezone> tzs = client.getDb().timeZone(targetMember.getId().asBigInteger());
                    Mono<Optional<Timezone>> tz =
                        tzs.map(Optional::of).single(Optional.empty());

                    return tz.flatMap(tzr -> {
                        if(tzr.isEmpty()) {
                            Optional<String> omsg =
                                client.getLangKeyHandler().format(userLocale, "error.no_timezone_registered", targetMember.getDisplayName());

                            // TODO missing key handling
                            String msg = omsg.orElse("error.no_timezone_registered");

                            return c.createMessage(msg).then(Mono.empty());
                        } else {
                            Timezone tzi = tzr.get();

                            ChronoFormatter<Moment> f =
                                ChronoFormatter.ofMomentStyle(DisplayMode.FULL, DisplayMode.FULL, userLocale, tzi.getID());

                            Moment now = Moment.nowInSystemTime();

                            String format = f.format(now);

                            Optional<String> omsg =
                                client.getLangKeyHandler().format(userLocale, "command.time.time_for_user", targetMember.getDisplayName(), format);

                            // TODO missing key handling
                            String msg = omsg.orElse("command.time.time_for_user");

                            return c.createMessage(msg).then(Mono.empty());
                        }
                    });
                } else if(mqr instanceof ChalkBotClient.MemberQueryResponse.User) {
                    User targetUser = ((ChalkBotClient.MemberQueryResponse.User) mqr).user();

                    Flux<Timezone> tzs = client.getDb().timeZone(targetUser.getId().asBigInteger());
                    Mono<Optional<Timezone>> tz =
                        tzs.map(Optional::of).single(Optional.empty());

                    return tz.flatMap(tzr -> {
                        if(tzr.isEmpty()) {
                            Optional<String> omsg =
                                client.getLangKeyHandler().format(userLocale, "error.no_timezone_registered", targetUser.getTag());

                            // TODO missing key handling
                            String msg = omsg.orElse("error.no_timezone_registered");

                            return c.createMessage(msg).then(Mono.empty());
                        } else {
                            Timezone tzi = tzr.get();

                            ChronoFormatter<Moment> f =
                                ChronoFormatter.ofMomentStyle(DisplayMode.FULL, DisplayMode.FULL, userLocale, tzi.getID());

                            Moment now = Moment.nowInSystemTime();

                            String format = f.format(now);

                            Optional<String> omsg =
                                client.getLangKeyHandler().format(userLocale, "command.time.time_for_user", targetUser.getTag(), format);

                            // TODO missing key handling
                            String msg = omsg.orElse("command.time.time_for_user");

                            return c.createMessage(msg).then(Mono.empty());
                        }
                    });
                } else {
                    if(!arguments.isEmpty()) {
                        Optional<String> omsg =
                            client.getLangKeyHandler().format(userLocale, "error.unknown_user", arguments);

                        // TODO missing key handling
                        String msg = omsg.orElse("error.unknown_user");

                        return c.createMessage(msg).then(Mono.empty());
                    } else {
                        return Mono.empty();
                    }
                }
            });
        });
    }
}
