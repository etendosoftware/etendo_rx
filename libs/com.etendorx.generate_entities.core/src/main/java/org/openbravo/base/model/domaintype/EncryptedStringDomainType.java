/*
 * Copyright 2022  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbravo.base.model.domaintype;

/**
 * The type for storing passwords. The value is encrypted before being saved in the database. The
 * cleartext value can be recovered by calling the {@link org.etendorx.utils.CryptoUtility#decrypt
 * decrypt} function.
 *
 * @author shuehner
 * @see org.etendorx.utils.CryptoUtility#encrypt
 */
public class EncryptedStringDomainType extends StringDomainType {

}
