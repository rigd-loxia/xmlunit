/*
  This file is licensed to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

using System.Collections;
using System.Collections.Generic;

namespace net.sf.xmlunit.util {
    /// <summary>
    /// Conversion methods.
    /// </summary>
    public sealed class Linqy {
        public static IEnumerable<T> Cast<T>(IEnumerable i) {
            foreach (T t in i) {
                yield return t;
            }
        }

        public static IEnumerable<T> Singleton<T>(T t) {
            yield return t;
        }

        public delegate T Mapper<F, T>(F from);

        public static IEnumerable<T> Map<F, T>(IEnumerable<F> from,
                                               Mapper<F, T> mapper) {
            foreach (F f in from) {
                yield return mapper(f);
            }
        }

        public delegate bool Predicate<T>(T toTest);

        public static IEnumerable<T> Filter<T>(IEnumerable<T> sequence,
                                               Predicate<T> filter) {
            foreach (T t in sequence) {
                if (filter(t)) {
                    yield return t;
                }
            }
        }

        public static int Count(IEnumerable e) {
            int c = 0;
            foreach (object o in e) {
                c++;
            }
            return c;
        }
    }
}