package com.example.DemoBot.configuration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry;


@Component
public class TelegramBotCommandInitializer implements InitializingBean {
    private final ICommandRegistry ICommandRegistry;
    private final IBotCommand[] IBotCommands;

    public TelegramBotCommandInitializer(ICommandRegistry iCommandRegistry,
                                         IBotCommand... iBotCommands) {
        ICommandRegistry = iCommandRegistry;
        IBotCommands = iBotCommands;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ICommandRegistry.registerAll(IBotCommands);
    }
}
