/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.block;

import io.smilo.commons.block.data.Parser;
import io.smilo.commons.block.data.Validator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provides the correct parser implementation based on class
 */
@Component
public class ParserProvider {

    private final List<Parser> providers;

    private final List<Validator> validators;

    public ParserProvider(List<Parser> providers, List<Validator> validators) {
        this.providers = providers;
        this.validators = validators;
    }

    /**
     * Returns the parser based on class
     * @param clazz class to query the known parsers for
     * @return the correct parser
     * @throws IllegalArgumentException when no parser is known for the given class
     */
    public Parser getParser(Class<?> clazz) {
        return providers.stream().filter(p -> p.supports(clazz)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unable to find matching parser!"));
    }

    public Validator getValidator(Class<?> clazz) {
        return validators.stream().filter(p -> p.supports(clazz)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unable to find matching validator!"));
    }
}
