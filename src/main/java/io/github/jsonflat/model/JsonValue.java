package io.github.jsonflat.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * @author Evgeniy Chukanov
 */

@lombok.Value
public class JsonValue implements Value {
	JsonNode value;
	boolean required;

	public JsonValue(JsonNode value, boolean required) {
		this.required = required;
		this.value = value;
	}

	@Override
	public boolean isEmpty() {
		return value == null || value.isNull();
	}
}
