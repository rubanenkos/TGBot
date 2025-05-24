package com.example.DemoBot.configuration;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.ICommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TelegramBotCommandInitializer implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotCommandInitializer.class);
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
        logger.info("Registered {} bot commands", IBotCommands.length);
    }
}
