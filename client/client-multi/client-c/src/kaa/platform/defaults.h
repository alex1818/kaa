/*
 * Copyright 2015 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
@file defaults.h
 Created on: Mar 26, 2015
     Author: Andriy Panasenko <apanasenko@cybervisiontech.com>
*/

#ifndef DEFAULTS_H_
#define DEFAULTS_H_

#ifdef ECONAIS_PLATFORM
#include "../platform-impl/Econais/EC19D/econais_ec19d_defaults.h"
#else
#include "../platform-impl/posix/posix_defaults.h"
#endif


#endif /* DEFAULTS_H_ */
